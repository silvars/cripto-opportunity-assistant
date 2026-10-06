package com.btcassistant.market.binance;

import com.btcassistant.market.Candle;
import com.btcassistant.market.CandleListener;
import com.btcassistant.market.Instrument;
import com.btcassistant.market.IntervalSupport;
import com.btcassistant.market.MarketDataProvider;
import com.btcassistant.market.MarketStatusRegistry;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementação inicial de MarketDataProvider (SDD §12). WebSocket para candles ao vivo (apenas
 * klines fechados, x=true), REST para backfill histórico/recuperação de gaps. Sem tipos reativos
 * (ADR-009) — callback simples entregue em thread própria do cliente WebSocket do JDK.
 */
@Component
public class BinanceMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(BinanceMarketDataProvider.class);
    private static final String WS_BASE_URL = "wss://stream.binance.com:9443/ws/";
    private static final String REST_BASE_URL = "https://api.binance.com/api/v3/klines";
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(30);
    private static final int REST_PAGE_LIMIT = 1000;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    private final MarketStatusRegistry statusRegistry;

    public BinanceMarketDataProvider(MarketStatusRegistry statusRegistry) {
        this.statusRegistry = statusRegistry;
    }

    @Override
    public void streamLive(Instrument instrument, CandleListener listener) {
        connect(instrument, listener, INITIAL_BACKOFF);
    }

    private void connect(Instrument instrument, CandleListener listener, Duration backoff) {
        String streamName = instrument.symbol().toLowerCase() + "@kline_" + instrument.interval();
        URI uri = URI.create(WS_BASE_URL + streamName);

        httpClient.newWebSocketBuilder()
                .buildAsync(uri, new BinanceWebSocketListener(instrument, listener, backoff))
                .exceptionally(throwable -> {
                    log.warn("websocket_disconnected symbol={} reason={}", instrument.symbol(), throwable.getMessage());
                    statusRegistry.websocketConnected(instrument.symbol(), false);
                    scheduleReconnect(instrument, listener, backoff);
                    return null;
                });
    }

    private void scheduleReconnect(Instrument instrument, CandleListener listener, Duration backoff) {
        Duration nextBackoff = backoff.compareTo(MAX_BACKOFF) >= 0 ? MAX_BACKOFF : backoff.multipliedBy(2);
        log.info("websocket_reconnecting symbol={} delaySeconds={}", instrument.symbol(), backoff.getSeconds());
        reconnectExecutor.schedule(() -> connect(instrument, listener, nextBackoff), backoff.getSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public List<Candle> fetchHistorical(Instrument instrument, Instant from, Instant to) {
        List<Candle> result = new ArrayList<>();
        Instant cursor = from;

        while (cursor.isBefore(to)) {
            PageResult page = fetchPage(instrument, cursor, to);
            if (page == null) {
                break;
            }
            result.addAll(page.candles());
            if (!page.hasMore()) {
                break;
            }
            cursor = page.nextCursor();
        }
        return result;
    }

    private record PageResult(List<Candle> candles, Instant nextCursor, boolean hasMore) {
    }

    private PageResult fetchPage(Instrument instrument, Instant cursor, Instant to) {
        String url = REST_BASE_URL
                + "?symbol=" + instrument.symbol()
                + "&interval=" + instrument.interval()
                + "&startTime=" + cursor.toEpochMilli()
                + "&endTime=" + to.toEpochMilli()
                + "&limit=" + REST_PAGE_LIMIT;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();

        try {
            HttpResponse<String> response = sendWithRateLimitRetry(request);
            return parsePage(instrument, cursor, response);
        } catch (IOException e) {
            log.warn("Binance REST backfill error: {}", e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private HttpResponse<String> sendWithRateLimitRetry(HttpRequest request) throws IOException, InterruptedException {
        while (true) {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 429 && response.statusCode() != 418) {
                return response;
            }
            long retryAfterSeconds = response.headers().firstValueAsLong("Retry-After").orElse(5);
            log.warn("Binance REST rate limited (status={}), retrying after {}s", response.statusCode(), retryAfterSeconds);
            sleep(Duration.ofSeconds(retryAfterSeconds));
        }
    }

    private PageResult parsePage(Instrument instrument, Instant cursor, HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            log.warn("Binance REST backfill failed with status {}", response.statusCode());
            return null;
        }

        JsonNode array = objectMapper.readTree(response.body());
        if (!array.isArray() || array.isEmpty()) {
            return null;
        }

        List<Candle> candles = new ArrayList<>();
        for (JsonNode row : array) {
            candles.add(BinanceKlineMapper.fromRest(instrument, row));
        }

        long lastOpenTimeMs = array.get(array.size() - 1).get(0).asLong();
        Instant next = Instant.ofEpochMilli(lastOpenTimeMs).plus(IntervalSupport.toDuration(instrument.interval()));
        boolean hasMore = next.isAfter(cursor) && array.size() >= REST_PAGE_LIMIT;
        return new PageResult(candles, next, hasMore);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    private final class BinanceWebSocketListener implements WebSocket.Listener {

        private final Instrument instrument;
        private final CandleListener listener;
        private final Duration backoff;
        private final StringBuilder buffer = new StringBuilder();

        private BinanceWebSocketListener(Instrument instrument, CandleListener listener, Duration backoff) {
            this.instrument = instrument;
            this.listener = listener;
            this.backoff = backoff;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("websocket_connected symbol={}", instrument.symbol());
            statusRegistry.websocketConnected(instrument.symbol(), true);
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String message = buffer.toString();
                buffer.setLength(0);
                try {
                    handleMessage(message);
                } catch (JacksonException e) {
                    log.warn("Failed to parse Binance kline message for {}: {}", instrument.symbol(), e.getMessage());
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.warn("websocket_disconnected symbol={} reason={}", instrument.symbol(), error.getMessage());
            statusRegistry.websocketConnected(instrument.symbol(), false);
            scheduleReconnect(instrument, listener, backoff);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.warn("websocket_disconnected symbol={} statusCode={} reason={}", instrument.symbol(), statusCode, reason);
            statusRegistry.websocketConnected(instrument.symbol(), false);
            scheduleReconnect(instrument, listener, backoff);
            return null;
        }

        private void handleMessage(String message) {
            JsonNode root = objectMapper.readTree(message);
            JsonNode kline = root.path("k");
            if (kline.isMissingNode()) {
                return;
            }
            boolean closed = kline.path("x").asBoolean(false);
            if (!closed) {
                return; // ignora candle parcial — só processa quando fechado
            }
            listener.onCandle(BinanceKlineMapper.fromWebSocket(instrument, kline));
        }
    }
}

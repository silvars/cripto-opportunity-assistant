package com.btcassistant.feature;

import com.btcassistant.market.Candle;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Janela de candles em memória, ordenada ascendentemente por {@code openTime}, terminando no
 * candle de referência (o candle em {@code T} para o qual as features estão sendo calculadas).
 *
 * <p>Nunca contém candles com {@code openTime} posterior ao de referência — quem monta a janela
 * (via consulta limitada por {@code to = referenceOpenTime}) garante a regra de no-lookahead
 * (SDD §2.2); esta classe apenas organiza o acesso, sem buscar dados adicionais.</p>
 */
public final class RollingWindow {

    private final List<Candle> candlesAscending;
    private final Map<Instant, Candle> byOpenTime;

    private RollingWindow(List<Candle> candlesAscending) {
        this.candlesAscending = candlesAscending;
        this.byOpenTime = candlesAscending.stream()
                .collect(Collectors.toMap(Candle::openTime, c -> c));
    }

    public static RollingWindow of(List<Candle> candlesAscending) {
        return new RollingWindow(candlesAscending);
    }

    /** O candle mais recente da janela — representa o instante {@code T} do cálculo. */
    public Candle reference() {
        return candlesAscending.get(candlesAscending.size() - 1);
    }

    /** Busca exata por {@code openTime} — candles são sempre alinhados ao boundary do intervalo. */
    public Optional<Candle> at(Instant openTime) {
        return Optional.ofNullable(byOpenTime.get(openTime));
    }

    /** Candles com {@code openTime >= from}, incluindo o de referência. */
    public List<Candle> since(Instant from) {
        return candlesAscending.stream().filter(c -> !c.openTime().isBefore(from)).toList();
    }

    /** Os {@code n} candles mais recentes (incluindo o de referência), ou menos se a janela for menor. */
    public List<Candle> lastN(int n) {
        int fromIndex = Math.max(0, candlesAscending.size() - n);
        return candlesAscending.subList(fromIndex, candlesAscending.size());
    }

    public int size() {
        return candlesAscending.size();
    }

    public List<Candle> all() {
        return candlesAscending;
    }

    public Duration span() {
        return Duration.between(candlesAscending.get(0).openTime(), reference().openTime());
    }
}

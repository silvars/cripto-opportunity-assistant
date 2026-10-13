"use client";

import { useEffect, useRef, useState } from "react";
import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { CandleResponse, MarketStatus } from "./types";
import { API_BASE_URL } from "./api";

interface MarketSocketState {
  candle: CandleResponse | null;
  status: MarketStatus | null;
  socketConnected: boolean;
}

/** Assina os tópicos STOMP do cockpit (SDD §51) — candle fechado e status de ingestão em tempo real. */
export function useMarketSocket(symbol: string): MarketSocketState {
  const [candle, setCandle] = useState<CandleResponse | null>(null);
  const [status, setStatus] = useState<MarketStatus | null>(null);
  const [socketConnected, setSocketConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
      reconnectDelay: 3000,
      onConnect: () => {
        setSocketConnected(true);
        client.subscribe(`/topic/market/${symbol}`, (message: IMessage) => {
          setCandle(JSON.parse(message.body) as CandleResponse);
        });
        client.subscribe(`/topic/market-status/${symbol}`, (message: IMessage) => {
          setStatus(JSON.parse(message.body) as MarketStatus);
        });
      },
      onDisconnect: () => setSocketConnected(false),
      onWebSocketClose: () => setSocketConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      void client.deactivate();
      clientRef.current = null;
    };
  }, [symbol]);

  return { candle, status, socketConnected };
}

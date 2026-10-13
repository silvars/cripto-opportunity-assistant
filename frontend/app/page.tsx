"use client";

import { useEffect, useState } from "react";
import { fetchLatestCandle, fetchStatus } from "@/lib/api";
import { useMarketSocket } from "@/lib/useMarketSocket";
import type { CandleResponse, MarketStatus } from "@/lib/types";
import { MarketCard } from "@/components/MarketCard";
import { StatusCard } from "@/components/StatusCard";

const SYMBOL = "BTCUSDT";

export default function Home() {
  const [initialCandle, setInitialCandle] = useState<CandleResponse | null>(null);
  const [initialStatus, setInitialStatus] = useState<MarketStatus | null>(null);
  const { candle: liveCandle, status: liveStatus, socketConnected } = useMarketSocket(SYMBOL);

  useEffect(() => {
    fetchLatestCandle(SYMBOL).then(setInitialCandle);
    fetchStatus(SYMBOL).then(setInitialStatus);
  }, []);

  const candle = liveCandle ?? initialCandle;
  const status = liveStatus ?? initialStatus;

  return (
    <main className="min-h-screen bg-slate-950 p-8 text-slate-100">
      <header className="mb-8">
        <h1 className="text-2xl font-semibold">BTC Intelligence Cockpit</h1>
        <p className="text-sm text-slate-500">Fase 0 — Walking Skeleton</p>
      </header>

      <section className="grid gap-6 md:grid-cols-2">
        <MarketCard symbol={SYMBOL} candle={candle} />
        <StatusCard status={status} socketConnected={socketConnected} />
      </section>
    </main>
  );
}

import type { MarketStatus } from "@/lib/types";

function Row({ label, value, ok }: { label: string; value: string; ok: boolean }) {
  return (
    <div className="flex items-center justify-between border-b border-slate-800 py-2 last:border-0">
      <dt className="text-slate-400">{label}</dt>
      <dd className={ok ? "font-medium text-emerald-400" : "font-medium text-amber-400"}>{value}</dd>
    </div>
  );
}

export function StatusCard({ status, socketConnected }: { status: MarketStatus | null; socketConnected: boolean }) {
  return (
    <div className="rounded-lg border border-slate-800 bg-slate-900 p-6">
      <h2 className="text-sm uppercase tracking-wide text-slate-400">Status da ingestão</h2>
      <dl className="mt-3">
        <Row label="WebSocket" value={socketConnected ? "CONNECTED" : "DISCONNECTED"} ok={socketConnected} />
        <Row
          label="Histórico"
          value={status?.historicalDataReady ? "READY" : "INCOMPLETE"}
          ok={Boolean(status?.historicalDataReady)}
        />
        <Row label="Fase" value={status?.phase ?? "—"} ok={status?.phase === "LIVE"} />
        <Row label="Candles armazenados" value={status ? status.candleCount.toLocaleString() : "—"} ok />
        <Row
          label="Histórico desde"
          value={status?.requiredHistoryStart ? new Date(status.requiredHistoryStart).toLocaleDateString("pt-BR") : "—"}
          ok
        />
      </dl>
      {!status && <p className="mt-3 text-xs text-slate-500">Status inicial ainda não carregado.</p>}
    </div>
  );
}

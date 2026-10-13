import type { CandleResponse } from "@/lib/types";

export function MarketCard({ symbol, candle }: { symbol: string; candle: CandleResponse | null }) {
  return (
    <div className="rounded-lg border border-slate-800 bg-slate-900 p-6">
      <h2 className="text-sm uppercase tracking-wide text-slate-400">{symbol}</h2>
      {candle ? (
        <>
          <p className="mt-2 text-4xl font-bold">
            $
            {Number(candle.close).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </p>
          <dl className="mt-4 grid grid-cols-2 gap-2 text-sm text-slate-400">
            <dt>Abertura</dt>
            <dd className="text-right text-slate-200">{Number(candle.open).toLocaleString()}</dd>
            <dt>Máxima</dt>
            <dd className="text-right text-slate-200">{Number(candle.high).toLocaleString()}</dd>
            <dt>Mínima</dt>
            <dd className="text-right text-slate-200">{Number(candle.low).toLocaleString()}</dd>
            <dt>Volume</dt>
            <dd className="text-right text-slate-200">{Number(candle.volume).toLocaleString()}</dd>
          </dl>
          <p className="mt-4 text-xs text-slate-500">
            Último candle fechado ({candle.interval}): {new Date(candle.openTime).toLocaleString("pt-BR")}
          </p>
        </>
      ) : (
        <p className="mt-4 text-slate-500">Aguardando primeiro candle...</p>
      )}
    </div>
  );
}

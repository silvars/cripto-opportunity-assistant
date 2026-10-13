export type MarketPhase =
  | "INITIALIZING"
  | "CHECKING_HISTORY"
  | "BACKFILLING"
  | "RETRYING"
  | "HISTORICAL_DATA_READY"
  | "LIVE";

export interface MarketStatus {
  symbol: string;
  phase: MarketPhase;
  websocketConnected: boolean;
  historicalDataReady: boolean;
  candleCount: number;
  earliestCandle: string | null;
  latestCandle: string | null;
  requiredHistoryStart: string | null;
  updatedAt: string;
}

export interface CandleResponse {
  symbol: string;
  interval: string;
  openTime: string;
  closeTime: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

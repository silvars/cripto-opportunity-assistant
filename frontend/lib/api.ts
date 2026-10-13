import type { CandleResponse, MarketStatus } from "./types";

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export async function fetchLatestCandle(symbol: string): Promise<CandleResponse | null> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/market/${symbol}/latest`, { cache: "no-store" });
    if (!response.ok) {
      return null;
    }
    const data = (await response.json()) as CandleResponse | null;
    return data ?? null;
  } catch {
    return null;
  }
}

export async function fetchStatus(symbol: string): Promise<MarketStatus | null> {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/market/${symbol}/status`, { cache: "no-store" });
    if (!response.ok) {
      return null;
    }
    return (await response.json()) as MarketStatus;
  } catch {
    return null;
  }
}

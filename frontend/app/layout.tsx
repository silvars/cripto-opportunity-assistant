import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "BTC Intelligence Cockpit",
  description: "Dashboard de acompanhamento BTC/USDT em tempo real (Fase 0 — Walking Skeleton)",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <body>{children}</body>
    </html>
  );
}

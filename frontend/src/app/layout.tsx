import type { Metadata } from "next";
import { Providers } from "./providers";

export const metadata: Metadata = {
  title: "Fixna LocalBoost",
  description:
    "AI-assisted local advertising orchestration for SMBs — plan, launch and measure hyperlocal campaigns.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <header>
          <nav aria-label="Primary">
            <a href="/">Fixna LocalBoost</a>
          </nav>
        </header>
        <main>
          <Providers>{children}</Providers>
        </main>
      </body>
    </html>
  );
}

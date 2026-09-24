import type { Metadata } from "next";
import { AppShell } from "@/components/app-shell";
import "./globals.css";
import "./product.css";
import "./workspace.css";
import "./responsive.css";
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
        <Providers><AppShell>{children}</AppShell></Providers>
      </body>
    </html>
  );
}


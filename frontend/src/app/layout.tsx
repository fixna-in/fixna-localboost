import type { Metadata, Viewport } from "next";
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
  applicationName: "Fixna LocalBoost",
};

export const viewport: Viewport = {
  themeColor: "#163e32",
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


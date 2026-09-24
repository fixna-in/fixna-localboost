"use client";

import type { ReactNode } from "react";
import { useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/lib/auth-context";
import { Brand } from "./ui";
import { toApiError } from "@/lib/api-client";

const navigation = [
  { href: "/dashboard", label: "Overview", icon: "◫" },
  { href: "/businesses", label: "Businesses", icon: "▦" },
  { href: "/campaigns", label: "Campaigns", icon: "◎" },
  { href: "/leads", label: "Leads", icon: "♧" },
];

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const { session, initialized, logout } = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const publicPage = ["/", "/login", "/register"].includes(pathname);
  const current = navigation.find(({ href }) => pathname === href || pathname.startsWith(`${href}/`));

  if (publicPage) return <>
    <a className="skip-link" href="#main-content">Skip to content</a>
    <header className="public-header"><Brand /><nav aria-label="Primary"><Link href="/dashboard">Workspace</Link><Link href="/login">Sign in</Link><Link className="button button-small" href="/register">Get started <span aria-hidden="true">↗</span></Link></nav></header>
    <main id="main-content" tabIndex={-1} className="public-content">{children}</main>
    <footer className="public-footer"><span>© {new Date().getFullYear()} Fixna · LocalBoost</span><span>Built for local ambition.</span></footer>
  </>;

  return <div className="workspace-shell">
    <a className="skip-link" href="#main-content">Skip to content</a>
    <aside className="sidebar">
      <Brand />
      <div className="workspace-label"><span className="workspace-avatar" aria-hidden="true">LB</span><div><strong>LocalBoost</strong><small>Your business workspace</small></div></div>
      <p className="nav-label">WORKSPACE</p>
      <nav aria-label="Primary">{navigation.map(({ href, label, icon }) => {
        const active = pathname === href || pathname.startsWith(`${href}/`);
        return <Link key={href} href={href} aria-current={active ? "page" : undefined}><span aria-hidden="true">{icon}</span>{label}</Link>;
      })}</nav>
      <div className="sidebar-note"><span className="eyebrow">YOU’RE IN CONTROL</span><p>AI recommends.<br />You make the decisions.</p><small>Review and approve every campaign before launch.</small></div>
      <div className="sidebar-account"><span className="workspace-avatar" aria-hidden="true">F</span><div><strong>{session ? "Your account" : "Welcome to Fixna"}</strong><small>{session ? session.role.toLowerCase().replaceAll("_", " ") : initialized ? "Sign in to get started" : "Restoring session…"}</small></div></div>
      {session ? <button className="button-quiet" disabled={busy} onClick={async () => {
        setBusy(true); setError(null);
        try { await logout(); await queryClient.cancelQueries(); queryClient.clear(); router.push("/login"); }
        catch (err) { setError(toApiError(err).message); }
        finally { setBusy(false); }
      }}>{busy ? "Signing out…" : "Sign out"}</button> : <Link className="button button-secondary" href="/login">Sign in</Link>}
      {error ? <p role="alert">{error}</p> : null}
    </aside>
    <div className="workspace-body"><header className="workspace-topbar"><div><span className="muted">Workspace</span><span className="breadcrumb-divider">/</span><strong>{current?.label ?? "Overview"}</strong></div><Link className="button button-small" href="/campaigns/new">+ Create campaign</Link></header><main id="main-content" tabIndex={-1} className="page-content">{children}</main><footer className="workspace-footer">Fixna LocalBoost <span>Plan thoughtfully. Grow locally.</span></footer></div>
  </div>;
}

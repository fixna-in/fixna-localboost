import type { ReactNode } from "react";
import Link from "next/link";
import { BrandMarkIcon } from "@/brand/brand-mark-icon";

export function Brand() {
  return (
    <Link className="brand" href="/" aria-label="Fixna LocalBoost home">
      <BrandMarkIcon className="brand-mark" />
      <span>fixna<span className="brand-product">LocalBoost</span></span>
    </Link>
  );
}

export function PageHeader({ eyebrow = "YOUR WORKSPACE", title, description, action }: {
  eyebrow?: string; title: string; description: string; action?: ReactNode;
}) {
  return <div className="page-heading"><div><p className="eyebrow">{eyebrow}</p><h1>{title}</h1><p className="muted">{description}</p></div>{action}</div>;
}

export function EmptyState({ title, description, href, action }: {
  title: string; description: string; href?: string; action?: string;
}) {
  return <div className="empty-state"><span className="empty-symbol" aria-hidden="true">＋</span><h3>{title}</h3><p>{description}</p>{href && action ? <Link className="button" href={href}>{action}</Link> : null}</div>;
}

export function MetricCard({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return <div className="metric-card"><dt>{label}</dt><dd>{value}</dd>{hint ? <p>{hint}</p> : null}</div>;
}

export function StatusBadge({ value }: { value: string }) {
  const tone = ["ACTIVE", "APPROVED", "CONVERTED", "COMPLETED"].includes(value) ? "positive" : ["FAILED", "LOST"].includes(value) ? "negative" : "neutral";
  return <span className={`status-badge ${tone}`}>{value.toLowerCase().replaceAll("_", " ")}</span>;
}

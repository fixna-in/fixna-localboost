"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { getDashboard, seedDemoMetrics } from "@/lib/insight-api";
import { useAuth } from "@/lib/auth-context";
import { useState } from "react";
import { ErrorState, LoadingState } from "../providers";
import { PageHeader, MetricCard, EmptyState } from "@/components/ui";
import { formatMetric } from "@/lib/display";

export default function DashboardPage() {
  const { session, initialized } = useAuth();
  const [seedMsg, setSeedMsg] = useState<string | null>(null);
  const [seedError, setSeedError] = useState<unknown>(null);
  const [seeding, setSeeding] = useState(false);
  const dashboard = useQuery({
    queryKey: ["dashboard"],
    queryFn: getDashboard,
    enabled: initialized && session !== null,
    retry: false,
  });

  if (!initialized) return <LoadingState label="Loading session" />;
  if (!session)
    return (
      <section aria-labelledby="dash-auth">
        <h1 id="dash-auth">Dashboard</h1>
        <p>
          Please <Link href="/login">sign in</Link> to view your tenant
          dashboard.
        </p>
      </section>
    );
  if (dashboard.isLoading) return <LoadingState label="Loading dashboard" />;
  if (dashboard.error) return <ErrorState error={dashboard.error} />;
  const d = dashboard.data;
  if (!d) return <LoadingState label="Loading dashboard" />;

  return (
    <section aria-label="Dashboard overview">
      <PageHeader title="Your growth, at a glance." description="A clear view of your campaigns and customer enquiries." />
      <div className="insight-banner"><span aria-hidden="true">✦</span><div><strong>Make your next local move.</strong><p>Build a campaign around your business, audience, and goals. Review every recommendation before launch.</p></div><Link className="button button-secondary" href="/campaigns/new">Create campaign ↗</Link></div>
      <p className="section-caption">{d.from && d.to ? `${d.from} — ${d.to}` : "Reporting period supplied by your workspace"}</p>
      <dl className="metrics-grid">
        <MetricCard label="Impressions" value={formatMetric(d.totals.impressions)} hint="Times your ads were shown" />
        <MetricCard label="Reach" value={formatMetric(d.totals.reach)} hint="Audience reached" />
        <MetricCard label="Clicks" value={formatMetric(d.totals.clicks)} hint="Interactions with your ads" />
        <MetricCard label="Conversions" value={formatMetric(d.totals.conversions)} hint="Reported campaign conversions" />
        <MetricCard label="Spend" value={formatMetric(d.totals.spend)} hint="Reported amount · currency not supplied" />
        <MetricCard label="Leads" value={formatMetric(d.totals.leads)} hint="Reported campaign leads" />
      </dl>
      <div className="dashboard-grid"><section className="panel"><div className="panel-heading"><h2>Campaign performance</h2><Link href="/campaigns">View all ↗</Link></div>
        {d.campaigns.length ? <div className="table-scroll" tabIndex={0} role="region" aria-label="Campaign performance"><table><thead><tr><th scope="col">Campaign</th><th scope="col">Impressions</th><th scope="col">Clicks</th><th scope="col">Leads</th></tr></thead><tbody>{d.campaigns.map((c) => <tr key={c.campaignId}><td><Link href={`/campaigns/${c.campaignId}`}>{c.name}</Link></td><td>{formatMetric(c.totals.impressions)}</td><td>{formatMetric(c.totals.clicks)}</td><td>{formatMetric(c.totals.leads)}</td></tr>)}</tbody></table></div> : <EmptyState title="Your next chapter starts here" description="Create a campaign to start building your performance overview." href="/campaigns/new" action="Create your first campaign" />}
      </section><section className="panel"><div className="panel-heading"><h2>Lead pipeline</h2><Link href="/leads">View leads ↗</Link></div><dl className="pipeline-list">{[["New enquiries", d.leads.newCount], ["Contacted", d.leads.contacted], ["Qualified", d.leads.qualified], ["Converted", d.leads.converted], ["Lost", d.leads.lost]].map(([label, count]) => <div key={String(label)}><dt>{label}</dt><dd>{formatMetric(count)}</dd></div>)}</dl></section></div>
      <section className="demo-tools"><div><h2>Demo data</h2><p>Populate sample metrics for testing. These are not live advertising results.</p></div><button
        type="button"
        className="button-secondary"
        disabled={seeding}
        onClick={async () => {
          setSeedMsg(null); setSeedError(null); setSeeding(true);
          try {
            await seedDemoMetrics();
            await dashboard.refetch();
            setSeedMsg("Demo metrics updated.");
          } catch (err) { setSeedError(err); }
          finally { setSeeding(false); }
        }}
      >{seeding ? "Loading demo data…" : "Load demo metrics"}</button></section>
      {seedError ? <ErrorState error={seedError} /> : null}
      {seedMsg ? <p role="status">{seedMsg}</p> : null}
    </section>
  );
}

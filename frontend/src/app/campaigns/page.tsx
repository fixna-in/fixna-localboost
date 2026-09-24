"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { listBusinesses } from "@/lib/tenant-business-api";
import { listCampaigns } from "@/lib/campaign-api";
import { ErrorState, LoadingState } from "../providers";
import { PageHeader, EmptyState, StatusBadge } from "@/components/ui";
import { formatMetric } from "@/lib/display";

export default function CampaignsPage() {
  const campaigns = useQuery({
    queryKey: ["campaigns"],
    queryFn: () => listCampaigns(),
    retry: false,
  });
  const businesses = useQuery({
    queryKey: ["businesses"],
    queryFn: listBusinesses,
    retry: false,
  });
  const businessName = (id: string) =>
    businesses.data?.find((b) => b.id === id)?.name ?? id;

  return (
    <section aria-label="Campaigns">
      <PageHeader title="Campaigns" description="From your next idea to your next local customer." action={<Link className="button" href="/campaigns/new">+ New campaign</Link>} />
      {campaigns.isLoading ? (
        <LoadingState label="Loading campaigns" />
      ) : campaigns.error ? (
        <ErrorState error={campaigns.error} />
      ) : (
        campaigns.data?.length ? <div className="panel table-scroll" role="region" aria-label="Campaign list" tabIndex={0}><table><thead><tr><th scope="col">Campaign</th><th scope="col">Business</th><th scope="col">Status</th><th scope="col">Budget</th></tr></thead><tbody>{campaigns.data.map((c) => <tr key={c.id}><td><Link href={`/campaigns/${c.id}`}>{c.name}</Link><small className="cell-description">{c.objective.toLowerCase().replaceAll("_", " ")}</small></td><td>{businessName(c.businessId)}</td><td><StatusBadge value={c.status} /></td><td>{c.currency ?? ""} {formatMetric(c.totalBudget)}</td></tr>)}</tbody></table></div> : <div className="panel"><EmptyState title="Make your first local move" description="Create a campaign to define your audience, budget, and goals." href="/campaigns/new" action="Create campaign" /></div>
      )}
    </section>
  );
}

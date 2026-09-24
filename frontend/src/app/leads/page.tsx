"use client";

import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { listLeads, updateLeadStatus } from "@/lib/insight-api";
import { ErrorState, LoadingState } from "../providers";
import { PageHeader, EmptyState, StatusBadge } from "@/components/ui";

export default function LeadsPage() {
  const [status, setStatus] = useState<string>("");
  const [error, setError] = useState<unknown>(null);
  const leads = useQuery({
    queryKey: ["leads", status],
    queryFn: () =>
      listLeads({ status: status || undefined, page: 0, size: 20 }),
    retry: false,
  });

  return (
    <section aria-label="Leads">
      <PageHeader title="Turn interest into opportunity." description="Keep track of the people taking their next step with your business." />
      <label className="filter-control">
        Filter by status
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All</option>
          <option value="NEW">NEW</option>
          <option value="CONTACTED">CONTACTED</option>
          <option value="QUALIFIED">QUALIFIED</option>
          <option value="CONVERTED">CONVERTED</option>
          <option value="LOST">LOST</option>
        </select>
      </label>
      {error ? <ErrorState error={error} /> : null}
      {leads.isLoading ? (
        <LoadingState label="Loading leads" />
      ) : leads.error ? (
        <ErrorState error={leads.error} />
      ) : (
        <>
          {!leads.data?.items.length ? <EmptyState title="Room for your next customer" description="No leads match this view. Your campaign enquiries will appear here." /> : null}
          <ul className="resource-list">
            {(leads.data?.items ?? []).map((l) => (
              <li key={l.id}>
                <h2>{l.name ?? l.phone ?? l.id}</h2>
                <p><StatusBadge value={l.status} /></p>
                <button
                  type="button"
                  onClick={async () => {
                    setError(null);
                    try {
                      await updateLeadStatus(l.id, "CONTACTED");
                      leads.refetch();
                    } catch (err) {
                      setError(err);
                    }
                  }}
                >
                  Mark contacted
                </button>
              </li>
            ))}
          </ul>
          <p role="status">
            Page {leads.data?.page ?? 0} of {leads.data?.totalPages ?? 0} ·{" "}
            {leads.data?.totalElements ?? 0} total
          </p>
        </>
      )}
    </section>
  );
}

"use client";

import { useParams } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import {
  executeLaunch,
  getCampaign,
  launchCampaign,
  replaceChannels,
  transitionCampaign,
  type CampaignStatus,
} from "@/lib/campaign-api";
import {
  listAudiences,
  listCreatives,
  listGeoTargets,
} from "@/lib/targeting-api";
import { requestRecommendation } from "@/lib/insight-api";
import { ErrorState, LoadingState } from "../../providers";
import { GeoTargetForm } from "./geo-form";
import { SubResourceButtons } from "./sub-resources";

const NEXT_STEP: Partial<Record<CampaignStatus, CampaignStatus>> = {
  DRAFT: "READY_FOR_REVIEW",
  READY_FOR_REVIEW: "APPROVED",
  APPROVED: "QUEUED",
  FAILED: "QUEUED",
};

export default function CampaignDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const qc = useQueryClient();
  const [error, setError] = useState<unknown>(null);
  const [aiJson, setAiJson] = useState<string | null>(null);

  const campaign = useQuery({
    queryKey: ["campaign", id],
    queryFn: () => getCampaign(id),
  });
  const geo = useQuery({
    queryKey: ["geo", id],
    queryFn: () => listGeoTargets(id),
  });
  const audiences = useQuery({
    queryKey: ["audiences", id],
    queryFn: () => listAudiences(id),
  });
  const creatives = useQuery({
    queryKey: ["creatives", id],
    queryFn: () => listCreatives(id),
  });

  const invalidateAll = () => {
    qc.invalidateQueries({ queryKey: ["campaign", id] });
    qc.invalidateQueries({ queryKey: ["geo", id] });
    qc.invalidateQueries({ queryKey: ["audiences", id] });
    qc.invalidateQueries({ queryKey: ["creatives", id] });
  };

  const transition = useMutation({
    mutationFn: (to: CampaignStatus) => transitionCampaign(id, to),
    onSuccess: invalidateAll,
    onError: setError,
  });
  const launch = useMutation({
    mutationFn: () => launchCampaign(id),
    onSuccess: invalidateAll,
    onError: setError,
  });
  const execute = useMutation({
    mutationFn: () => executeLaunch(id),
    onSuccess: invalidateAll,
    onError: setError,
  });

  if (campaign.isLoading) return <LoadingState label="Loading campaign" />;
  if (campaign.error) return <ErrorState error={campaign.error} />;
  const c = campaign.data;
  if (!c) return <LoadingState label="Loading campaign" />;
  const next = NEXT_STEP[c.status];

  return (
    <section aria-labelledby="camp-detail">
      <h1 id="camp-detail">
        {c.name} · {c.status}
      </h1>
      <p>
        Budget: {String(c.totalBudget)} {c.currency ?? "INR"} · Objective:{" "}
        {c.objective}
      </p>
      {error ? <ErrorState error={error} /> : null}
      <h2>Lifecycle</h2>
      {next ? (
        <button
          type="button"
          disabled={transition.isPending}
          onClick={() => transition.mutate(next)}
        >
          Advance to {next}
        </button>
      ) : null}
      {c.status === "APPROVED" || c.status === "FAILED" ? (
        <button
          type="button"
          disabled={launch.isPending}
          onClick={() => launch.mutate()}
        >
          Request launch (idempotent)
        </button>
      ) : null}
      {c.status === "QUEUED" ? (
        <button
          type="button"
          disabled={execute.isPending}
          onClick={() => execute.mutate()}
        >
          Execute launch (mock adapters)
        </button>
      ) : null}
      <button
        type="button"
        onClick={async () => {
          setError(null);
          try {
            await replaceChannels(id, [
              { channel: "GOOGLE", allocatedBudget: Number(c.totalBudget) / 2 },
              { channel: "META", allocatedBudget: Number(c.totalBudget) / 2 },
            ]);
          } catch (err) {
            setError(err);
          }
        }}
      >
        Split budget GOOGLE/META 50-50
      </button>
      <h2>AI recommendation (advisory only)</h2>
      <button
        type="button"
        onClick={async () => {
          setError(null);
          try {
            const r = await requestRecommendation({
              type: "CAMPAIGN_STRATEGY",
              businessId: c.businessId,
              campaignId: c.id,
              payload: {
                objective: c.objective,
                totalBudget: Number(c.totalBudget),
              },
            });
            setAiJson(JSON.stringify(r, null, 2));
          } catch (err) {
            setError(err);
          }
        }}
      >
        Get strategy recommendation
      </button>
      {aiJson ? <pre>{aiJson}</pre> : null}
      <h2>Geo targets</h2>
      {geo.isLoading ? (
        <LoadingState label="Loading geo targets" />
      ) : geo.error ? (
        <ErrorState error={geo.error} />
      ) : (
        <ul>
          {(geo.data ?? []).map((g) => (
            <li key={g.id}>
              {g.targetType} {g.city ?? g.postalCode ?? g.name ?? ""}
            </li>
          ))}
        </ul>
      )}
      <GeoTargetForm campaignId={id} onError={setError} />
      <SubResourceButtons
        campaignId={id}
        audiences={audiences.data ?? []}
        audiencesLoading={audiences.isLoading}
        audiencesError={audiences.error}
        creatives={creatives.data ?? []}
        creativesLoading={creatives.isLoading}
        creativesError={creatives.error}
        onError={setError}
      />
    </section>
  );
}

"use client";

import { useQueryClient } from "@tanstack/react-query";
import { seedDemoMetrics } from "@/lib/insight-api";
import {
  createAudience,
  createCreative,
  type Audience,
  type Creative,
} from "@/lib/targeting-api";
import { ErrorState, LoadingState } from "../../providers";

export function SubResourceButtons({
  campaignId,
  audiences,
  audiencesLoading,
  audiencesError,
  creatives,
  creativesLoading,
  creativesError,
  onError,
}: {
  campaignId: string;
  audiences: Audience[];
  audiencesLoading: boolean;
  audiencesError: unknown;
  creatives: Creative[];
  creativesLoading: boolean;
  creativesError: unknown;
  onError: (e: unknown) => void;
}) {
  const qc = useQueryClient();
  return (
    <>
      <h2>Audiences</h2>
      {audiencesLoading ? (
        <LoadingState label="Loading audiences" />
      ) : audiencesError ? (
        <ErrorState error={audiencesError} />
      ) : (
        <ul>
          {audiences.map((a) => (
            <li key={a.id}>{a.name}</li>
          ))}
        </ul>
      )}
      <button
        type="button"
        onClick={async () => {
          try {
            await createAudience(campaignId, {
              name: "Local families",
              definition: { ageMin: 25, ageMax: 45, interests: ["local"] },
            });
            qc.invalidateQueries({ queryKey: ["audiences", campaignId] });
          } catch (err) {
            onError(err);
          }
        }}
      >
        Add sample audience
      </button>
      <h2>Creatives</h2>
      {creativesLoading ? (
        <LoadingState label="Loading creatives" />
      ) : creativesError ? (
        <ErrorState error={creativesError} />
      ) : (
        <ul>
          {creatives.map((cr) => (
            <li key={cr.id}>
              {cr.channel}: {cr.headline ?? cr.body ?? cr.id} ({cr.status})
            </li>
          ))}
        </ul>
      )}
      <button
        type="button"
        onClick={async () => {
          try {
            await createCreative(campaignId, {
              channel: "META",
              headline: "Festive offer near you",
              body: "Walk in this week for 20% off.",
              callToAction: "Visit store",
              status: "DRAFT",
            });
            qc.invalidateQueries({ queryKey: ["creatives", campaignId] });
          } catch (err) {
            onError(err);
          }
        }}
      >
        Add sample creative
      </button>
      <h2>Demo metrics</h2>
      <button
        type="button"
        onClick={async () => {
          try {
            await seedDemoMetrics(campaignId);
          } catch (err) {
            onError(err);
          }
        }}
      >
        Seed demo metrics
      </button>
    </>
  );
}

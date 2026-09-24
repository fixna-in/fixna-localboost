"use client";

import { useQuery } from "@tanstack/react-query";
import { useParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import {
  LocationInputSchema,
  addLocation,
  getBusiness,
  listLocations,
  type LocationInput,
} from "@/lib/tenant-business-api";
import { listCampaigns as listAllCampaigns } from "@/lib/campaign-api";
import { ErrorState, LoadingState } from "../../providers";
import Link from "next/link";

export default function BusinessDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const [error, setError] = useState<unknown>(null);
  const business = useQuery({
    queryKey: ["business", id],
    queryFn: () => getBusiness(id),
  });
  const locations = useQuery({
    queryKey: ["business-locations", id],
    queryFn: () => listLocations(id),
  });
  const campaigns = useQuery({
    queryKey: ["business-campaigns", id],
    queryFn: () => listAllCampaigns(id),
  });
  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting },
  } = useForm<LocationInput>({ resolver: zodResolver(LocationInputSchema) });

  if (business.isLoading) return <LoadingState label="Loading business" />;
  if (business.error) return <ErrorState error={business.error} />;

  return (
    <section aria-labelledby="biz-detail">
      <h1 id="biz-detail">{business.data?.name}</h1>
      <h2>Locations</h2>
      {locations.isLoading ? (
        <LoadingState label="Loading locations" />
      ) : locations.error ? (
        <ErrorState error={locations.error} />
      ) : (
        <ul>
          {(locations.data ?? []).map((l) => (
            <li key={l.id}>
              {l.addressLine ?? l.city ?? l.id} {l.city ? `· ${l.city}` : ""}
            </li>
          ))}
        </ul>
      )}
      <form
        onSubmit={handleSubmit(async (values) => {
          setError(null);
          try {
            await addLocation(id, values);
            reset();
            locations.refetch();
          } catch (err) {
            setError(err);
          }
        })}
      >
        <label>
          Address
          <input {...register("addressLine")} />
        </label>
        <label>
          City
          <input {...register("city")} />
        </label>
        <label>
          Postal code
          <input {...register("postalCode")} />
        </label>
        <button type="submit" disabled={isSubmitting}>
          Add location
        </button>
      </form>
      {error ? <ErrorState error={error} /> : null}
      <h2>Campaigns</h2>
      {campaigns.isLoading ? (
        <LoadingState label="Loading campaigns" />
      ) : campaigns.error ? (
        <ErrorState error={campaigns.error} />
      ) : (
        <ul>
          {(campaigns.data ?? []).map((c) => (
            <li key={c.id}>
              <Link href={`/campaigns/${c.id}`}>
                {c.name} · {c.status}
              </Link>
            </li>
          ))}
        </ul>
      )}
      <p>
        <Link href={`/campaigns/new?businessId=${id}`}>New campaign</Link>
      </p>
    </section>
  );
}

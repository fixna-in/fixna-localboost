"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  CampaignInputSchema,
  createCampaign,
  type CampaignInput,
} from "@/lib/campaign-api";
import { listBusinesses } from "@/lib/tenant-business-api";
import { ErrorState, LoadingState } from "../../providers";

export function NewCampaignForm() {
  const router = useRouter();
  const search = useSearchParams();
  const presetBusiness = search.get("businessId") ?? "";
  const [error, setError] = useState<unknown>(null);
  const businesses = useQuery({
    queryKey: ["businesses"],
    queryFn: listBusinesses,
  });
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CampaignInput>({
    resolver: zodResolver(CampaignInputSchema),
    defaultValues: { businessId: presetBusiness, currency: "INR" },
  });

  if (businesses.isLoading) return <LoadingState label="Loading businesses" />;
  if (businesses.error) return <ErrorState error={businesses.error} />;

  return (
    <section aria-labelledby="new-camp">
      <h1 id="new-camp">New campaign</h1>
      <form
        onSubmit={handleSubmit(async (values) => {
          setError(null);
          try {
            const created = await createCampaign(values);
            router.push(`/campaigns/${created.id}`);
          } catch (err) {
            setError(err);
          }
        })}
      >
        <label>
          Business
          <select {...register("businessId")}>
            <option value="">Select…</option>
            {(businesses.data ?? []).map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
              </option>
            ))}
          </select>
        </label>
        {errors.businessId ? (
          <p role="alert">{errors.businessId.message}</p>
        ) : null}
        <label>
          Name
          <input {...register("name")} />
        </label>
        {errors.name ? <p role="alert">{errors.name.message}</p> : null}
        <label>
          Objective
          <select {...register("objective")}>
            <option value="LEAD_GENERATION">LEAD_GENERATION</option>
            <option value="STORE_VISITS">STORE_VISITS</option>
            <option value="WEBSITE_TRAFFIC">WEBSITE_TRAFFIC</option>
            <option value="WHATSAPP_ENQUIRIES">WHATSAPP_ENQUIRIES</option>
            <option value="PROMOTION">PROMOTION</option>
          </select>
        </label>
        <label>
          Total budget
          <input
            type="number"
            step="0.01"
            {...register("totalBudget", { valueAsNumber: true })}
          />
        </label>
        {errors.totalBudget ? (
          <p role="alert">{errors.totalBudget.message}</p>
        ) : null}
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Creating…" : "Create draft"}
        </button>
      </form>
      {error ? <ErrorState error={error} /> : null}
    </section>
  );
}
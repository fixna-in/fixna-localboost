"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useQueryClient } from "@tanstack/react-query";
import {
  GeoTargetInputSchema,
  addGeoTarget,
  type GeoTargetInput,
} from "@/lib/targeting-api";

export function GeoTargetForm({
  campaignId,
  onError,
}: {
  campaignId: string;
  onError: (e: unknown) => void;
}) {
  const qc = useQueryClient();
  const form = useForm<GeoTargetInput>({
    resolver: zodResolver(GeoTargetInputSchema),
    defaultValues: { targetType: "CITY" },
  });
  return (
    <form
      onSubmit={form.handleSubmit(async (values) => {
        try {
          await addGeoTarget(campaignId, values);
          form.reset({ targetType: "CITY" });
          qc.invalidateQueries({ queryKey: ["geo", campaignId] });
        } catch (err) {
          onError(err);
        }
      })}
    >
      <label>
        Type
        <select {...form.register("targetType")}>
          <option value="RADIUS">RADIUS</option>
          <option value="CITY">CITY</option>
          <option value="POSTAL">POSTAL</option>
          <option value="REGION">REGION</option>
          <option value="COUNTRY">COUNTRY</option>
        </select>
      </label>
      <label>
        City
        <input {...form.register("city")} />
      </label>
      <label>
        Postal code
        <input {...form.register("postalCode")} />
      </label>
      <label>
        Radius km
        <input
          type="number"
          step="0.1"
          {...form.register("radiusKm", { valueAsNumber: true })}
        />
      </label>
      <button type="submit">Add geo target</button>
    </form>
  );
}

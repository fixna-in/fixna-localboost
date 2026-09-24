import { z } from "zod";
import { apiClient } from "./api-client";

export const GeoTargetSchema = z.object({
  id: z.string(),
  targetType: z.enum(["RADIUS", "CITY", "POSTAL", "REGION", "COUNTRY"]),
  name: z.string().nullable().optional(),
  latitude: z.union([z.number(), z.string()]).nullable().optional(),
  longitude: z.union([z.number(), z.string()]).nullable().optional(),
  radiusKm: z.union([z.number(), z.string()]).nullable().optional(),
  countryCode: z.string().nullable().optional(),
  regionCode: z.string().nullable().optional(),
  city: z.string().nullable().optional(),
  postalCode: z.string().nullable().optional(),
});
export type GeoTarget = z.infer<typeof GeoTargetSchema>;

export const GeoTargetInputSchema = z.object({
  targetType: z.enum(["RADIUS", "CITY", "POSTAL", "REGION", "COUNTRY"]),
  name: z.string().optional(),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
  radiusKm: z.number().optional(),
  countryCode: z.string().optional(),
  regionCode: z.string().optional(),
  city: z.string().optional(),
  postalCode: z.string().optional(),
});
export type GeoTargetInput = z.infer<typeof GeoTargetInputSchema>;

export const AudienceSchema = z.object({
  id: z.string(),
  name: z.string(),
  definition: z.record(z.string(), z.unknown()),
});
export type Audience = z.infer<typeof AudienceSchema>;

export const CreativeSchema = z.object({
  id: z.string(),
  channel: z.string(),
  headline: z.string().nullable().optional(),
  body: z.string().nullable().optional(),
  callToAction: z.string().nullable().optional(),
  status: z.string(),
});
export type Creative = z.infer<typeof CreativeSchema>;

export async function listGeoTargets(campaignId: string): Promise<GeoTarget[]> {
  const { data } = await apiClient.get(`/campaigns/${campaignId}/geo-targets`);
  return z.array(GeoTargetSchema).parse(data);
}

export async function addGeoTarget(
  campaignId: string,
  input: GeoTargetInput,
): Promise<GeoTarget> {
  const { data } = await apiClient.post(
    `/campaigns/${campaignId}/geo-targets`,
    input,
  );
  return GeoTargetSchema.parse(data);
}

export async function listAudiences(campaignId: string): Promise<Audience[]> {
  const { data } = await apiClient.get(`/campaigns/${campaignId}/audiences`);
  return z.array(AudienceSchema).parse(data);
}

export async function createAudience(
  campaignId: string,
  input: { name: string; definition: Record<string, unknown> },
): Promise<Audience> {
  const { data } = await apiClient.post(
    `/campaigns/${campaignId}/audiences`,
    input,
  );
  return AudienceSchema.parse(data);
}

export async function listCreatives(campaignId: string): Promise<Creative[]> {
  const { data } = await apiClient.get(`/campaigns/${campaignId}/creatives`);
  return z.array(CreativeSchema).parse(data);
}

export async function createCreative(
  campaignId: string,
  input: {
    channel: string;
    headline?: string;
    body?: string;
    callToAction?: string;
    status?: string;
  },
): Promise<Creative> {
  const { data } = await apiClient.post(
    `/campaigns/${campaignId}/creatives`,
    input,
  );
  return CreativeSchema.parse(data);
}

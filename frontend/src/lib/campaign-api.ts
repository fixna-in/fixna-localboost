import { z } from "zod";
import { apiClient } from "./api-client";

export const CampaignObjectiveSchema = z.enum([
  "LEAD_GENERATION",
  "STORE_VISITS",
  "WEBSITE_TRAFFIC",
  "WHATSAPP_ENQUIRIES",
  "PROMOTION",
]);
export type CampaignObjective = z.infer<typeof CampaignObjectiveSchema>;

export const CampaignStatusSchema = z.enum([
  "DRAFT",
  "READY_FOR_REVIEW",
  "APPROVED",
  "QUEUED",
  "CREATING",
  "ACTIVE",
  "PAUSED",
  "COMPLETED",
  "FAILED",
]);
export type CampaignStatus = z.infer<typeof CampaignStatusSchema>;

export const CampaignSchema = z.object({
  id: z.string(),
  businessId: z.string(),
  name: z.string(),
  objective: CampaignObjectiveSchema,
  status: CampaignStatusSchema,
  totalBudget: z.union([z.number(), z.string()]),
  currency: z.string().optional(),
  startAt: z.string().nullable().optional(),
  endAt: z.string().nullable().optional(),
  externalReference: z.string().nullable().optional(),
});
export type Campaign = z.infer<typeof CampaignSchema>;

export const CampaignInputSchema = z.object({
  businessId: z.string().min(1),
  name: z.string().min(1).max(255),
  objective: CampaignObjectiveSchema,
  totalBudget: z.number().positive(),
  currency: z.string().length(3).optional(),
  startAt: z.string().optional(),
  endAt: z.string().optional(),
});
export type CampaignInput = z.infer<typeof CampaignInputSchema>;

export async function listCampaigns(businessId?: string): Promise<Campaign[]> {
  const { data } = await apiClient.get("/campaigns", {
    params: businessId ? { businessId } : undefined,
  });
  return z.array(CampaignSchema).parse(data);
}

export async function createCampaign(input: CampaignInput): Promise<Campaign> {
  const { data } = await apiClient.post("/campaigns", input);
  return CampaignSchema.parse(data);
}

export async function getCampaign(id: string): Promise<Campaign> {
  const { data } = await apiClient.get(`/campaigns/${id}`);
  return CampaignSchema.parse(data);
}

export async function transitionCampaign(
  id: string,
  to: CampaignStatus,
): Promise<Campaign> {
  const { data } = await apiClient.post(`/campaigns/${id}/transitions`, {
    to,
  });
  return CampaignSchema.parse(data);
}

export async function launchCampaign(id: string): Promise<Campaign> {
  const { data } = await apiClient.post(`/campaigns/${id}/launch`);
  return CampaignSchema.parse(data);
}

export async function executeLaunch(id: string): Promise<Campaign> {
  const { data } = await apiClient.post(`/campaigns/${id}/execute-launch`);
  return CampaignSchema.parse(data);
}

export async function replaceChannels(
  id: string,
  channels: { channel: string; allocatedBudget: number }[],
): Promise<unknown> {
  const { data } = await apiClient.put(`/campaigns/${id}/channels`, {
    channels,
  });
  return data;
}

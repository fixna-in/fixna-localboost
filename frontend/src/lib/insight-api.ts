import { z } from "zod";
import { apiClient } from "./api-client";

export const AiResultSchema = z.object({
  provider: z.string(),
  model: z.string(),
  promptVersion: z.string(),
  data: z.record(z.string(), z.unknown()),
  promptTokens: z.number(),
  completionTokens: z.number(),
  estimatedCostMicros: z.number(),
});
export type AiResult = z.infer<typeof AiResultSchema>;

export async function requestRecommendation(input: {
  type: "CAMPAIGN_STRATEGY" | "AUDIENCE" | "BUDGET_ALLOCATION" | "CREATIVE";
  businessId?: string;
  campaignId?: string;
  payload?: Record<string, unknown>;
}): Promise<AiResult> {
  const { data } = await apiClient.post("/ai/recommendations", input);
  return AiResultSchema.parse(data);
}

export const TotalsSchema = z.object({
  spend: z.union([z.number(), z.string()]).optional(),
  impressions: z.number().optional(),
  reach: z.number().optional(),
  clicks: z.number().optional(),
  conversions: z.number().optional(),
  leads: z.number().optional(),
});
export type Totals = z.infer<typeof TotalsSchema>;

export const DashboardSchema = z.object({
  from: z.string().nullable().optional(),
  to: z.string().nullable().optional(),
  totals: TotalsSchema,
  campaigns: z.array(
    z.object({
      campaignId: z.string(),
      name: z.string(),
      totals: TotalsSchema,
    }),
  ),
  leads: z.object({
    newCount: z.number(),
    contacted: z.number(),
    qualified: z.number(),
    converted: z.number(),
    lost: z.number(),
  }),
});
export type Dashboard = z.infer<typeof DashboardSchema>;

export async function getDashboard(): Promise<Dashboard> {
  const { data } = await apiClient.get("/analytics/dashboard");
  return DashboardSchema.parse(data);
}

export async function seedDemoMetrics(campaignId?: string): Promise<unknown> {
  const { data } = await apiClient.post(
    "/analytics/demo-seed",
    {},
    { params: campaignId ? { campaignId } : undefined },
  );
  return data;
}

export const LeadSchema = z.object({
  id: z.string(),
  businessId: z.string(),
  campaignId: z.string().nullable().optional(),
  name: z.string().nullable().optional(),
  phone: z.string().nullable().optional(),
  email: z.string().nullable().optional(),
  status: z.string(),
  source: z.string().nullable().optional(),
});
export type Lead = z.infer<typeof LeadSchema>;

export const LeadPageSchema = z.object({
  items: z.array(LeadSchema),
  page: z.number(),
  size: z.number(),
  totalElements: z.number(),
  totalPages: z.number(),
});
export type LeadPage = z.infer<typeof LeadPageSchema>;

export async function listLeads(params?: {
  businessId?: string;
  campaignId?: string;
  status?: string;
  page?: number;
  size?: number;
}): Promise<LeadPage> {
  const { data } = await apiClient.get("/leads", { params });
  return LeadPageSchema.parse(data);
}

export async function updateLeadStatus(
  id: string,
  status: string,
): Promise<Lead> {
  const { data } = await apiClient.patch(`/leads/${id}/status`, { status });
  return LeadSchema.parse(data);
}

import { z } from "zod";
import { apiClient } from "./api-client";

/**
 * Tenant + business APIs — mirrors TenantController and BusinessController.
 * Tenant scope always derives from the JWT; no tenantId is sent.
 */

export const TenantSchema = z.object({
  id: z.string(),
  name: z.string(),
  type: z.string().optional(),
});
export type Tenant = z.infer<typeof TenantSchema>;

export const MembershipSchema = z.object({
  userId: z.string(),
  email: z.string().optional(),
  role: z.string(),
});
export type Membership = z.infer<typeof MembershipSchema>;

export const BusinessSchema = z.object({
  id: z.string(),
  name: z.string(),
  category: z.string().nullable().optional(),
  description: z.string().nullable().optional(),
  websiteUrl: z.string().nullable().optional(),
  phone: z.string().nullable().optional(),
});
export type Business = z.infer<typeof BusinessSchema>;

export const BusinessInputSchema = z.object({
  name: z.string().min(1).max(255),
  category: z.string().max(100).optional(),
  description: z.string().optional(),
  websiteUrl: z.string().max(1000).optional(),
  phone: z.string().max(50).optional(),
});
export type BusinessInput = z.infer<typeof BusinessInputSchema>;

export const LocationSchema = z.object({
  id: z.string(),
  addressLine: z.string().nullable().optional(),
  city: z.string().nullable().optional(),
  state: z.string().nullable().optional(),
  postalCode: z.string().nullable().optional(),
  country: z.string().nullable().optional(),
  latitude: z.number().nullable().optional(),
  longitude: z.number().nullable().optional(),
});
export type BusinessLocation = z.infer<typeof LocationSchema>;

export const LocationInputSchema = z.object({
  addressLine: z.string().max(500).optional(),
  city: z.string().max(150).optional(),
  state: z.string().max(150).optional(),
  postalCode: z.string().max(30).optional(),
  country: z.string().max(100).optional(),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
});
export type LocationInput = z.infer<typeof LocationInputSchema>;

export async function getCurrentTenant(): Promise<Tenant> {
  const { data } = await apiClient.get("/tenants/current");
  return TenantSchema.parse(data);
}

export async function listMembers(): Promise<Membership[]> {
  const { data } = await apiClient.get("/tenants/current/members");
  return z.array(MembershipSchema).parse(data);
}

export async function listBusinesses(): Promise<Business[]> {
  const { data } = await apiClient.get("/businesses");
  return z.array(BusinessSchema).parse(data);
}

export async function createBusiness(input: BusinessInput): Promise<Business> {
  const { data } = await apiClient.post("/businesses", input);
  return BusinessSchema.parse(data);
}

export async function getBusiness(id: string): Promise<Business> {
  const { data } = await apiClient.get(`/businesses/${id}`);
  return BusinessSchema.parse(data);
}

export async function updateBusiness(
  id: string,
  input: BusinessInput,
): Promise<Business> {
  const { data } = await apiClient.put(`/businesses/${id}`, input);
  return BusinessSchema.parse(data);
}

export async function listLocations(businessId: string): Promise<BusinessLocation[]> {
  const { data } = await apiClient.get(`/businesses/${businessId}/locations`);
  return z.array(LocationSchema).parse(data);
}

export async function addLocation(
  businessId: string,
  input: LocationInput,
): Promise<BusinessLocation> {
  const { data } = await apiClient.post(
    `/businesses/${businessId}/locations`,
    input,
  );
  return LocationSchema.parse(data);
}

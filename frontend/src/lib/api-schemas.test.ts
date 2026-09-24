import { describe, expect, it } from "vitest";
import { LoginSchema, AuthResponseSchema } from "@/lib/auth-api";
import { CampaignInputSchema } from "@/lib/campaign-api";
import { toApiError } from "@/lib/api-client";

describe("auth schemas", () => {
  it("rejects invalid login", () => {
    expect(LoginSchema.safeParse({ email: "x", password: "" }).success).toBe(
      false,
    );
  });

  it("parses an auth response", () => {
    expect(
      AuthResponseSchema.safeParse({
        accessToken: "a",
        refreshToken: "r",
        tokenType: "Bearer",
        expiresInSeconds: 900,
        userId: "u1",
        tenantId: "t1",
        role: "TENANT_OWNER",
      }).success,
    ).toBe(true);
  });
});

describe("campaign input", () => {
  it("rejects non-positive budget", () => {
    expect(
      CampaignInputSchema.safeParse({
        businessId: "b1",
        name: "x",
        objective: "PROMOTION",
        totalBudget: 0,
      }).success,
    ).toBe(false);
  });
});

describe("api error mapping", () => {
  it("maps network failures", () => {
    expect(toApiError(new Error("boom")).code).toBe("NETWORK_ERROR");
  });
});

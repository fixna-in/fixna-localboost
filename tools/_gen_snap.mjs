#!/usr/bin/env node
/**
 * Generates the importable Postman collection for the Fixna LocalBoost
 * backend API v1 (docs/09-postman/fixna-localboost-api-v1.postman_collection.json).
 *
 * The collection mirrors the implemented Spring controllers; it is produced by
 * script so the JSON is always importable and regenerating after an endpoint
 * change is a one-liner:
 *
 *   node tools/generate-postman-collection.mjs
 *
 * Scope: the tenant always comes from the JWT (never from a payload/query
 * field) — the variables below are usability helpers, never authorization
 * inputs.
 */
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const OUT_FILE = resolve(
  here,
  "..",
  "docs",
  "09-postman",
  "fixna-localboost-api-v1.postman_collection.json",
);

/** Splits an absolute path into Postman's host/path pair. */
function urlOf(path, query) {
  const clean = path.replace(/^\//, "");
  const url = {
    raw: "{{baseUrl}}/" + clean,
    host: ["{{baseUrl}}"],
    path: clean.split("/"),
  };
  if (query && query.length) {
    url.query = query.map(([key, value]) => ({ key, value, disabled: false }));
    url.raw = url.raw + "?" + query.map(([k, v]) => `${k}=${v}`).join("&");
  }
  return url;
}

/** Builds one Postman request item. */
function req({ name, method, path, desc, body, query, noauth, tests }) {
  const request = { method, url: urlOf(path, query) };
  if (desc) request.description = desc;
  if (noauth) request.auth = { type: "noauth" };
  if (body) {
    request.header = [{ key: "Content-Type", value: "application/json" }];
    request.body = {
      mode: "raw",
      raw: JSON.stringify(body, null, 2),
      options: { raw: { language: "json" } },
    };
  }
  const item = { name, request };
  if (tests && tests.length) {
    item.event = [
      { listen: "test", script: { type: "text/javascript", exec: tests } },
    ];
  }
  return item;
}

function folder(name, description, item) {
  return { name, description, item };
}

const ok200 = "pm.test('200 OK', function () { pm.response.to.have.status(200); });";
const created201 = "pm.test('201 Created', function () { pm.response.to.have.status(201); });";
const noContent204 = "pm.test('204 No Content', function () { pm.response.to.have.status(204); });";
const accepted202 = "pm.test('202 Accepted', function () { pm.response.to.have.status(202); });";
const bodyJson = "pm.test('JSON body returned', function () { pm.response.to.be.json; });";

/** Saves a response field into a collection variable (usability only). */
function saveVar(field, variable) {
  return (
    "const data = pm.response.json();\n" +
    `if (data && data.${field}) { pm.collectionVariables.set('${variable}', data.${field}); }`
  );
}

/** Captures the bearer/refresh token pair from an auth response. */
const saveTokens = [
  "const data = pm.response.json();",
  "if (data && data.accessToken) {",
  "  pm.collectionVariables.set('accessToken', data.accessToken);",
  "  pm.collectionVariables.set('refreshToken', data.refreshToken);",
  "  if (data.userId) { pm.collectionVariables.set('userId', data.userId); }",
  "  if (data.tenantId) { pm.collectionVariables.set('tenantId', data.tenantId); }",
  "  if (data.role) { pm.collectionVariables.set('role', data.role); }",
  "}",
];

const FOLDERS = [];

/** Postman collection variables. Usability helpers only — never authorization. */
const VARIABLES = [
  { key: "baseUrl", value: "http://localhost:8080", type: "string" },
  { key: "accessToken", value: "", type: "string" },
  { key: "refreshToken", value: "", type: "string" },
  { key: "userId", value: "", type: "string" },
  { key: "tenantId", value: "", type: "string" },
  { key: "role", value: "", type: "string" },
  { key: "businessId", value: "", type: "string" },
  { key: "campaignId", value: "", type: "string" },
  { key: "geoTargetId", value: "", type: "string" },
  { key: "audienceId", value: "", type: "string" },
  { key: "creativeId", value: "", type: "string" },
  { key: "leadId", value: "", type: "string" },
];

// 00 — Health (public probes) -----------------------------------------------

FOLDERS.push(
  folder("00 Health", "Unauthenticated liveness/readiness probes.", [
    req({
      name: "Liveness",
      method: "GET",
      path: "/api/v1/health",
      noauth: true,
      desc: "Process liveness. Never touches the database.",
      tests: [ok200, "pm.test('status UP', function () { pm.expect(pm.response.json().status).to.eql('UP'); });"],
    }),
    req({
      name: "Readiness",
      method: "GET",
      path: "/api/v1/health/readiness",
      noauth: true,
      desc: "Dependency readiness (database + migrations applied).",
      tests: [
        ok200,
        "pm.test('status READY', function () { pm.expect(pm.response.json().status).to.eql('READY'); });",
      ],
    }),
  ]),
);

// 01 — Auth ----------------------------------------------------------------

FOLDERS.push(
  folder("01 Auth", "Public auth surface: register, login, single-use refresh, logout.", [
    req({
      name: "Register (user + tenant)",
      method: "POST",
      path: "/api/v1/auth/register",
      noauth: true,
      desc: "Creates user + tenant + owner membership atomically (US-001/US-002). "
        + "Password min 8 chars; email is normalized server-side.",
      body: {
        email: "owner@fixna-demo.in",
        password: "Str0ngPassw0rd!",
        firstName: "Demo",
        lastName: "Owner",
        tenantName: "Fixna Demo Clinic",
      },
      tests: [created201, bodyJson, ...saveTokens],
    }),
    req({
      name: "Login",
      method: "POST",
      path: "/api/v1/auth/login",
      noauth: true,
      desc: "Tenant is selected server-side from memberships; failures share one generic message. "
        + "The request is rate limited per IP.",
      body: { email: "owner@fixna-demo.in", password: "Str0ngPassw0rd!" },
      tests: [ok200, bodyJson, ...saveTokens],
    }),
    req({
      name: "Refresh (rotate)",
      method: "POST",
      path: "/api/v1/auth/refresh",
      noauth: true,
      desc: "Refresh tokens are single-use: the presented token is revoked and a new pair is issued.",
      body: { refreshToken: "{{refreshToken}}" },
      tests: [ok200, bodyJson, ...saveTokens],
    }),
    req({
      name: "Logout (revoke all refresh tokens)",
      method: "POST",
      path: "/api/v1/auth/logout",
      desc: "Revokes every refresh token for the authenticated user. No request body.",
      tests: [noContent204],
    }),
  ]),
);

// 02 — Tenants --------------------------------------------------------------

FOLDERS.push(
  folder("02 Tenants", "Current tenant, tenant creation and membership management.", [
    req({
      name: "Current tenant",
      method: "GET",
      path: "/api/v1/tenants/current",
      desc: "Tenant resolved from the JWT principal — no tenantId is accepted from the client.",
      tests: [ok200, bodyJson],
    }),
    req({
      name: "Create tenant (caller becomes owner)",
      method: "POST",
      path: "/api/v1/tenants",
      desc: "Creates a tenant and an owner membership for the authenticated user.",
      body: { name: "Fixna Demo Agency" },
      tests: [created201, bodyJson],
    }),
    req({
      name: "List current tenant members (admin)",
      method: "GET",
      path: "/api/v1/tenants/current/members",
      desc: "Requires TENANT_OWNER/TENANT_ADMIN on the current tenant.",
      tests: [ok200, bodyJson],
    }),
    req({
      name: "Remove member (admin)",
      method: "DELETE",
      path: "/api/v1/tenants/current/members/{{userId}}",
      desc: "Removes a membership. The last owner cannot be removed (guarded server-side).",
      tests: [noContent204],
    }),
  ]),
);

// 03 — Businesses -----------------------------------------------------------

FOLDERS.push(
  folder("03 Businesses", "Tenant-scoped businesses and their locations.", [
    req({
      name: "List businesses",
      method: "GET",
      path: "/api/v1/businesses",
      desc: "All businesses of the current tenant.",
      tests: [ok200, bodyJson],
    }),
    req({
      name: "Create business",
      method: "POST",
      path: "/api/v1/businesses",
      desc: "Create a business for the current tenant (no tenant field in the payload).",
      body: {
        name: "Smile Dental Clinic",
        category: "CLINIC",
        description: "Family dental clinic in Noida",
        websiteUrl: "https://smile-dental.example.in",
        phone: "+91-9810000000",
      },
      tests: [created201, bodyJson, saveVar("id", "businessId")],
    }),
    req({
      name: "Get business by id",
      method: "GET",
      path: "/api/v1/businesses/{{businessId}}",
      desc: "Cross-tenant ids surface as 404 — never a cross-tenant read.",
      tests: [ok200, bodyJson],
    }),
    req({
      name: "Update business",
      method: "PUT",
      path: "/api/v1/businesses/{{businessId}}",
      desc: "Full replace of the mutable business fields.",
      body: {
        name: "Smile Dental Clinic (Noida)",
        category: "CLINIC",
        description: "Family dental clinic and orthodontics in Noida",
        websiteUrl: "https://smile-dental.example.in",
        phone: "+91-9810000001",
      },
      tests: [ok200, bodyJson],
    }),
    req({
      name: "List business locations",
      method: "GET",
      path: "/api/v1/businesses/{{businessId}}/locations",
      tests: [ok200, bodyJson],
    }),
    req({
      name: "Add business location",
      method: "POST",
      path: "/api/v1/businesses/{{businessId}}/locations",
      desc: "Physical location used for local targeting and lead attribution.",
      body: {
        addressLine: "A-12, Sector 18 Market",
        city: "Noida",
        state: "Uttar Pradesh",
        postalCode: "201301",
        country: "India",
        latitude: 28.5706,
        longitude: 77.3219,
      },
      tests: [created201, bodyJson],
    }),
    req({
      name: "Delete business",
      method: "DELETE",
      path: "/api/v1/businesses/{{businessId}}",
      desc: "Deleted after the dependent-location checks pass.",
      tests: [noContent204],
    }),
  ]),
);

// __APPEND__

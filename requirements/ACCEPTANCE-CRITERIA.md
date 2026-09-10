# Acceptance Criteria

## Tenant isolation
Given two tenants, resources created by tenant A are invisible and immutable to tenant B.

## Campaign
A valid campaign can progress from DRAFT to approval and then mock ACTIVE.

## AI
Invalid structured AI output is rejected. Valid output is validated against business rules.

## Launch
Repeating the same launch request does not create duplicate external campaigns.

## Demo mode
The full core journey works without real AI or advertising credentials.

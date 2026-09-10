# Business Rules

1. Campaign belongs to exactly one tenant and business.
2. Business belongs to exactly one tenant.
3. Campaign budget must be positive.
4. Channel allocations cannot exceed total budget.
5. Campaign cannot launch before approval.
6. Launch must be idempotent.
7. AI recommendation cannot mutate campaign state.
8. External provider failures move execution to a recoverable FAILED state.
9. Tenant limits are enforced server-side.
10. Every impactful action creates an audit event.

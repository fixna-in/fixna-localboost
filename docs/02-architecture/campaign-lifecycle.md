# Campaign Lifecycle

DRAFT
 -> READY_FOR_REVIEW
 -> APPROVED
 -> QUEUED
 -> CREATING
 -> ACTIVE
 -> PAUSED / COMPLETED

Failure path:
CREATING -> FAILED -> retry or manual intervention

Only approved campaigns can execute.
Launch is idempotent and external calls are not performed inside long DB transactions.

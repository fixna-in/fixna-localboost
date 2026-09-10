# Integration Strategy

MVP uses mocks for Google Ads, Meta Ads and WhatsApp.

Application depends on:
AdvertisingPlatformAdapter

Adapters map domain commands to provider-specific APIs.

Real provider credentials must never be hardcoded and should be stored encrypted.

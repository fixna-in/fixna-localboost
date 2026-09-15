package in.fixna.platform.campaign;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.campaign.dto.CampaignResponse;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.platform.AdvertisingPlatformAdapter;
import in.fixna.platform.platform.LaunchReceipt;
import in.fixna.platform.platform.PlatformAdapterRegistry;
import in.fixna.platform.platform.PlatformException;
import in.fixna.platform.platform.PlatformLaunchRequest;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Launch orchestration contract (BR-5/BR-6, ADC-005): idempotent no-op for
 * non-queued states, bounded retries for retryable provider errors only,
 * single attempt for permanent errors, and every terminal outcome committed
 * through the transactional edge bean.
 */
@ExtendWith(MockitoExtension.class)
class CampaignLaunchServiceTest {

    @Mock CampaignRepository campaigns;
    @Mock CampaignChannelRepository channels;
    @Mock PlatformAdapterRegistry registry;
    @Mock AdvertisingPlatformAdapter adapter;
    @Mock CampaignLaunchTx tx;
    @Mock in.fixna.platform.notification.NotificationService notifications;

    @InjectMocks CampaignLaunchService service;

    @Captor ArgumentCaptor<List<LaunchReceipt>> receiptsCaptor;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final String externalReference = "wf07-ref-1";

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private Campaign campaignOf(CampaignStatus status) {
        Campaign campaign = new Campaign();
        campaign.setId(campaignId);
        campaign.setTenantId(tenantId);
        campaign.setName("Diwali Offers");
        campaign.setStatus(status);
        campaign.setExternalReference(externalReference);
        campaign.setCurrency("INR");
        campaign.setTotalBudget(new BigDecimal("500.00"));
        return campaign;
    }

    private CampaignChannel channelOf(String name, String budget) {
        CampaignChannel channel = new CampaignChannel();
        channel.setTenantId(tenantId);
        channel.setCampaignId(campaignId);
        channel.setChannel(name);
        channel.setAllocatedBudget(new BigDecimal(budget));
        return channel;
    }

    @Test
    void unknownCampaignRejected() {
        TenantContext.set(tenantId, UUID.randomUUID(), MembershipRole.TENANT_OWNER);
        when(campaigns.findByIdAndTenantId(campaignId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.run(campaignId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CAMPAIGN_NOT_FOUND");
    }

    @Test
    void draftCampaignRejectedBeforeAnyExternalCall() {
        TenantContext.set(tenantId, UUID.randomUUID(), MembershipRole.TENANT_OWNER);
        when(campaigns.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(campaignOf(CampaignStatus.DRAFT)));

        assertThatThrownBy(() -> service.run(campaignId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CAMPAIGN_NOT_IN_QUEUE");
        verify(tx, never()).begin(any());
    }

    @Test
    void activeCampaignIsIdempotentNoOp() {
        TenantContext.set(tenantId, UUID.randomUUID(), MembershipRole.TENANT_OWNER);
        Campaign active = campaignOf(CampaignStatus.ACTIVE);
        when(campaigns.findByIdAndTenantId(campaignId, tenantId)).thenReturn(Optional.of(active));

        CampaignResponse response = service.run(campaignId);

        assertThat(response.status()).isEqualTo(CampaignStatus.ACTIVE);
        verify(tx, never()).begin(any());
        verify(channels, never()).findByTenantIdAndCampaignId(any(), any());
    }

    @Test
    void successfulLaunchCommitsActiveWithAllReceipts() {
        TenantContext.set(tenantId, UUID.randomUUID(), MembershipRole.TENANT_OWNER);
        Campaign queued = campaignOf(CampaignStatus.QUEUED);
        Campaign active = campaignOf(CampaignStatus.ACTIVE);
        when(campaigns.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(queued))
                .thenReturn(Optional.of(active));
        when(channels.findByTenantIdAndCampaignId(tenantId, campaignId)).thenReturn(List.of(
                channelOf("GOOGLE", "300.00"),
                channelOf("META", "200.00")));
        when(registry.forChannel("GOOGLE")).thenReturn(adapter);
        when(registry.forChannel("META")).thenReturn(adapter);
        when(adapter.launch(any(PlatformLaunchRequest.class)))
                .thenReturn(new LaunchReceipt("GOOGLE", "g-1", "ACTIVE", OffsetDateTime.now()))
                .thenReturn(new LaunchReceipt("META", "m-1", "ACTIVE", OffsetDateTime.now()));

        CampaignResponse response = service.run(campaignId);

        assertThat(response.status()).isEqualTo(CampaignStatus.ACTIVE);
        verify(tx).begin(campaignId);
        verify(tx).applyOutcome(eq(campaignId), receiptsCaptor.capture(), isNull(), isNull());
        assertThat(receiptsCaptor.getValue()).hasSize(2);
        assertThat(receiptsCaptor.getValue())
                .extracting(LaunchReceipt::platform)
                .containsExactly("GOOGLE", "META");
    }

    @Test
    void retryableFailureExhaustsThreeAttemptsThenFails() {
        TenantContext.set(tenantId, UUID.randomUUID(), MembershipRole.TENANT_OWNER);
        Campaign queued = campaignOf(CampaignStatus.QUEUED);
        Campaign failed = campaignOf(CampaignStatus.FAILED);
        when(campaigns.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(queued))
                .thenReturn(Optional.of(failed));
        when(channels.findByTenantIdAndCampaignId(tenantId, campaignId))
                .thenReturn(List.of(channelOf("GOOGLE", "500.00")));
        when(registry.forChannel("GOOGLE")).thenReturn(adapter);
        when(adapter.launch(any(PlatformLaunchRequest.class)))
                .thenAnswer(inv -> {
                    throw PlatformException.retryable("GOOGLE", "launch", "transient outage");
                });

        service.run(campaignId);

        verify(adapter, times(CampaignLaunchService.MAX_ATTEMPTS)).launch(any());
        verify(tx).applyOutcome(
                eq(campaignId), anyList(), eq("PROVIDER_RETRY_EXHAUSTED"), anyString());
    }

    @Test
    void permanentFailureStopsAfterSingleAttempt() {
        TenantContext.set(tenantId, UUID.randomUUID(), MembershipRole.TENANT_OWNER);
        Campaign queued = campaignOf(CampaignStatus.QUEUED);
        Campaign failed = campaignOf(CampaignStatus.FAILED);
        when(campaigns.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(queued))
                .thenReturn(Optional.of(failed));
        when(channels.findByTenantIdAndCampaignId(tenantId, campaignId))
                .thenReturn(List.of(channelOf("META", "500.00")));
        when(registry.forChannel("META")).thenReturn(adapter);
        when(adapter.launch(any(PlatformLaunchRequest.class)))
                .thenAnswer(inv -> {
                    throw PlatformException.permanent("META", "launch", "unsupported objective");
                });

        service.run(campaignId);

        verify(adapter, times(1)).launch(any());
        verify(tx).applyOutcome(
                eq(campaignId), anyList(), eq("PROVIDER_REJECTED"), anyString());
    }

    @Test
    void unknownChannelLandsInFailedOutcomeNotException() {
        TenantContext.set(tenantId, UUID.randomUUID(), MembershipRole.TENANT_OWNER);
        Campaign queued = campaignOf(CampaignStatus.QUEUED);
        Campaign failed = campaignOf(CampaignStatus.FAILED);
        when(campaigns.findByIdAndTenantId(campaignId, tenantId))
                .thenReturn(Optional.of(queued))
                .thenReturn(Optional.of(failed));
        when(channels.findByTenantIdAndCampaignId(tenantId, campaignId))
                .thenReturn(List.of(channelOf("TIKTOK", "500.00")));
        when(registry.forChannel("TIKTOK"))
                .thenThrow(PlatformException.permanent("TIKTOK", "resolve", "No adapter registered"));

        service.run(campaignId);

        verify(adapter, never()).launch(any());
        verify(tx).applyOutcome(
                eq(campaignId), anyList(), eq("PROVIDER_REJECTED"), anyString());
    }
}

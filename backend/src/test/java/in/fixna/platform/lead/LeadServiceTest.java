package in.fixna.platform.lead;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import in.fixna.platform.business.Business;
import in.fixna.platform.business.BusinessRepository;
import in.fixna.platform.campaign.CampaignRepository;
import in.fixna.platform.common.audit.AuditPublisher;
import in.fixna.platform.common.tenant.TenantContext;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.lead.dto.LeadPageResponse;
import in.fixna.platform.lead.dto.LeadRequest;
import in.fixna.platform.lead.dto.LeadStatusRequest;
import in.fixna.platform.tenant.MembershipRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Lead service tests: tenant isolation, RBAC, parent business/campaign
 * ownership, funnel terminal locking, pagination clamping.
 */
@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock LeadRepository leads;
    @Mock BusinessRepository businesses;
    @Mock CampaignRepository campaigns;
    @Mock AuditPublisher audit;
    @Mock in.fixna.platform.notification.NotificationService notifications;

    @InjectMocks LeadService service;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID userA = UUID.randomUUID();
    private final UUID businessId = UUID.randomUUID();
    private final UUID otherTenantCampaign = UUID.randomUUID();

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void createCapturesLeadInNewState() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_MARKETING_USER);
        when(businesses.findByIdAndTenantId(businessId, tenantA)).thenReturn(Optional.of(new Business()));
        when(leads.save(any())).thenAnswer(inv -> {
            Lead lead = inv.getArgument(0);
            lead.setId(UUID.randomUUID());
            return lead;
        });

        var response = service.create(
                businessId, new LeadRequest("Ravi", "9999999999", null, null, "WALKIN"));

        assertThat(response.status()).isEqualTo(LeadStatus.NEW);
        assertThat(response.businessId()).isEqualTo(businessId);
        verify(leads).save(any(Lead.class));
        verify(audit).publish(any());
    }

    @Test
    void createWithUnknownBusinessFails() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_MARKETING_USER);
        when(businesses.findByIdAndTenantId(businessId, tenantA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                        businessId, new LeadRequest("x", null, null, null, null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("BUSINESS_NOT_FOUND");
        verifyNoInteractions(leads);
    }

    @Test
    void createWithCrossTenantCampaignFails() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_MARKETING_USER);
        when(businesses.findByIdAndTenantId(businessId, tenantA)).thenReturn(Optional.of(new Business()));
        when(campaigns.findByIdAndTenantId(otherTenantCampaign, tenantA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(businessId, new LeadRequest(
                        "x", null, null, otherTenantCampaign, null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("CAMPAIGN_NOT_FOUND");
        verifyNoInteractions(leads);
    }

    @Test
    void viewerCannotCaptureLeads() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_VIEWER);

        assertThatThrownBy(() -> service.create(
                        businessId, new LeadRequest("x", null, null, null, null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(leads, businesses);
    }

    @Test
    void updateStatusTransitionsLead() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_MARKETING_USER);
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setTenantId(tenantA);
        lead.setStatus(LeadStatus.NEW);
        when(leads.findByIdAndTenantId(leadId, tenantA)).thenReturn(Optional.of(lead));
        when(leads.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.updateStatus(leadId, new LeadStatusRequest(LeadStatus.CONTACTED));

        assertThat(response.status()).isEqualTo(LeadStatus.CONTACTED);
        verify(leads).save(lead);
        verify(audit).publish(any());
    }

    @Test
    void updateStatusLockedInTerminalState() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_MARKETING_USER);
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setTenantId(tenantA);
        lead.setStatus(LeadStatus.CONVERTED);
        when(leads.findByIdAndTenantId(leadId, tenantA)).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> service.updateStatus(
                        leadId, new LeadStatusRequest(LeadStatus.CONTACTED)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("LEAD_TERMINAL");
        verify(leads, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void getIsTenantScoped() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_MARKETING_MANAGER);
        UUID leadId = UUID.randomUUID();
        when(leads.findByIdAndTenantId(leadId, tenantA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(leadId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("LEAD_NOT_FOUND");
    }

    @Test
    void listClampsPagination() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_MARKETING_USER);
        Lead lead = new Lead();
        lead.setId(UUID.randomUUID());
        lead.setTenantId(tenantA);
        lead.setBusinessId(businessId);
        lead.setName("Ravi");
        lead.setStatus(LeadStatus.NEW);
        when(leads.search(eq(tenantA), eq(businessId), isNull(), isNull(), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable clamped = inv.getArgument(4);
                    return new PageImpl<>(List.of(lead), clamped, 1);
                });

        LeadPageResponse response = service.list(businessId, null, null, -1, 500);

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(100);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).name()).isEqualTo("Ravi");
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(leads).search(eq(tenantA), eq(businessId), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void deleteRemovesOwnedLead() {
        TenantContext.set(tenantA, userA, MembershipRole.TENANT_MARKETING_MANAGER);
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setTenantId(tenantA);
        lead.setStatus(LeadStatus.NEW);
        when(leads.findByIdAndTenantId(leadId, tenantA)).thenReturn(Optional.of(lead));

        service.delete(leadId);

        verify(leads).delete(lead);
        verify(audit).publish(any());
    }
}

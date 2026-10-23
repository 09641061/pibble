package com.takodu.api.conversations.application.internal.queryservices;

import com.takodu.api.conversations.application.internal.outboundservices.acl.ExternalOrganizationService;
import com.takodu.api.conversations.application.queryservices.WhatsappLineQueryService;
import com.takodu.api.conversations.domain.model.aggregates.WhatsappLine;
import com.takodu.api.conversations.domain.model.queries.GetWhatsappLineByEstablishmentQuery;
import com.takodu.api.conversations.domain.repositories.WhatsappLineRepository;
import com.takodu.api.shared.application.tenancy.AccessPolicy;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class WhatsappLineQueryServiceImpl implements WhatsappLineQueryService {
    private final WhatsappLineRepository whatsappLineRepository;
    private final ExternalOrganizationService externalOrganizationService;
    private final AccessPolicy accessPolicy;

    public WhatsappLineQueryServiceImpl(WhatsappLineRepository whatsappLineRepository,
                                        ExternalOrganizationService externalOrganizationService,
                                        AccessPolicy accessPolicy) {
        this.whatsappLineRepository = whatsappLineRepository;
        this.externalOrganizationService = externalOrganizationService;
        this.accessPolicy = accessPolicy;
    }

    @Override
    public Optional<WhatsappLine> handle(GetWhatsappLineByEstablishmentQuery query) {
        externalOrganizationService.fetchEstablishment(query.organizationId(), query.establishmentId());
        accessPolicy.requireEstablishment(query.establishmentId());
        return whatsappLineRepository.findLatestByEstablishment(query.organizationId(), query.establishmentId());
    }
}

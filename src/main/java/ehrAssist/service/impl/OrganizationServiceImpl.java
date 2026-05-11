package ehrAssist.service.impl;

import ehrAssist.dto.projection.CareManagerOrganizationProjection;
import ehrAssist.mapper.OrganizationMapper;
import ehrAssist.repository.OrganizationRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.service.OrganizationService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final BundleBuilder bundleBuilder;
    private final PractitionerRepository practitionerRepository;

    @Override
    @Transactional(readOnly = true)
    public Bundle getOrganizationsByCareManager(UUID careManagerId, Pageable pageable) {
        practitionerRepository.findById(careManagerId)
                .orElseThrow(() -> new RuntimeException("Invalid!,Care Manager ID."));
        Page<CareManagerOrganizationProjection> data =
                organizationRepository.findOrganizationsByCareManagerId(careManagerId, pageable);
        List<CareManagerOrganizationProjection> organizations = data.getContent();

        if (!ObjectUtils.isEmpty(organizations)) {
            List<Resource> fhirResources = organizations.stream()
                    .map(organizationMapper::projectionToFhirResource)
                    .map(Resource.class::cast)
                    .toList();

            StringBuilder queryParams = new StringBuilder();
            if (careManagerId != null) queryParams.append("careManagerId=").append(careManagerId).append("&");
            String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

            return bundleBuilder.searchSetWithPagination("Organization", fhirResources, data.getTotalElements(),
                    pageable.getPageNumber(), pageable.getPageSize(), query);
        }

        return bundleBuilder.searchSetWithPagination("Organization", null, data.getTotalElements(),
                pageable.getPageNumber(), pageable.getPageSize(), "");
    }
}

package ehrAssist.service.impl;

import ehrAssist.dto.projection.CareManagerOrganizationProjection;
import ehrAssist.entity.PatientEntity;
import ehrAssist.mapper.OrganizationMapper;
import ehrAssist.mapper.PatientMapper;
import ehrAssist.repository.OrganizationRepository;
import ehrAssist.repository.PatientRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.service.OrganizationService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final PatientMapper patientMapper;
    private final BundleBuilder bundleBuilder;
    private final PractitionerRepository practitionerRepository;
    private final PatientRepository patientRepository;

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

    @Override
    @Transactional(readOnly = true)
    public Bundle fetchAllPatientsByOrganization(UUID careManagerId, UUID orgId, Pageable pageable) {
        organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("This Organization don't exist!."));

        boolean isAdmin = isCurrentUserAdmin();
        Page<PatientEntity> patientPage;
        if (isAdmin) {
            patientPage = patientRepository.findAllPatientsByOrganizationViaMapper(orgId, pageable);
        } else {
            patientPage = patientRepository.findPatientsByOrganizationViaMapper(careManagerId, orgId, pageable);
        }
        List<PatientEntity> patients = patientPage.getContent();

        String queryParams = "orgId=" + orgId;

        if (!ObjectUtils.isEmpty(patients)) {
            List<Resource> fhirResources = patients.stream()
                    .map(patientMapper::toFhirResource)
                    .map(Resource.class::cast)
                    .toList();

            return bundleBuilder.searchSetWithPagination("Patient", fhirResources, patientPage.getTotalElements(),
                    pageable.getPageNumber(), pageable.getPageSize(), queryParams);
        }

        return bundleBuilder.searchSetWithPagination("Patient", List.of(), patientPage.getTotalElements(),
                pageable.getPageNumber(), pageable.getPageSize(), queryParams);
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
    }
}

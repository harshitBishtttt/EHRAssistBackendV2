package ehrAssist.controller;

import ehrAssist.service.OrganizationService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/baseR4/Organization")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final FhirResponseHelper fhirResponseHelper;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER')")
    @GetMapping(value = "/by-care-manager", produces = "application/fhir+json")
    public ResponseEntity<String> getOrganizationsByCareManager(
            @RequestParam UUID _id,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Bundle bundle = organizationService.getOrganizationsByCareManager(_id, pageable);
        return fhirResponseHelper.toResponse(bundle);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER')")
    @GetMapping(value = "/patients", produces = "application/fhir+json")
    public ResponseEntity<String> fetchAllPatientsByOrganization(
            @RequestParam UUID orgId,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Bundle bundle = organizationService.fetchAllPatientsByOrganization(orgId, pageable);
        return fhirResponseHelper.toResponse(bundle);
    }
}

package ehrAssist.controller;

import ca.uhn.fhir.context.FhirContext;
import ehrAssist.dto.request.AiRecommendationInstructionsRequest;
import ehrAssist.dto.request.AiRecommendedActionRequest;
import ehrAssist.dto.request.CreateP360RiskScoreRequest;
import ehrAssist.dto.response.PatientsByPractitionerResponse;
import ehrAssist.dto.response.PractitionerDropdownResponse;
import ehrAssist.dto.response.ProviderRiskScoreResponse;
import ehrAssist.service.AiRecommendationInstructionsService;
import ehrAssist.service.AiRecommendedActionService;
import ehrAssist.service.PractitionerService;
import ehrAssist.util.FhirResponseHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.RiskAssessment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/baseR4/Practitioner")
@RequiredArgsConstructor
public class PractitionerController {

    private final PractitionerService practitionerService;
    private final AiRecommendationInstructionsService aiRecommendationInstructionsService;
    private final AiRecommendedActionService aiRecommendedActionService;
    private final FhirResponseHelper fhirResponseHelper;
    private final FhirContext fhirContext;


    // @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER')")  -> removing for now
    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getById(@PathVariable UUID id) {
        Practitioner practitioner = practitionerService.getById(id);
        return fhirResponseHelper.toResponse(practitioner);
    }

    // @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER')")
    @GetMapping(value = "/dropdown", produces = "application/json")
    public ResponseEntity<List<PractitionerDropdownResponse>> dropdown() {
        return ResponseEntity.ok(practitionerService.listPractitionerDropdown());
    }

    // @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER')")
    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> search(
            @RequestParam(required = false) UUID _id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String specialty,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Bundle bundle = practitionerService.search(_id, name, specialty, pageable);
        return fhirResponseHelper.toResponse(bundle);
    }

    @PostMapping(consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> create(@RequestBody String body) {
        Practitioner practitioner = fhirContext.newJsonParser().parseResource(Practitioner.class, body);
        Practitioner created = practitionerService.create(practitioner);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .header("Location", "/baseR4/Practitioner/" + created.getIdElement().getIdPart())
                .body(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created));
    }

    @PutMapping(value = "/{id}", consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> update(@PathVariable UUID id, @RequestBody String body) {
        Practitioner practitioner = fhirContext.newJsonParser().parseResource(Practitioner.class, body);
        Practitioner updated = practitionerService.update(id, practitioner);
        return fhirResponseHelper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        practitionerService.delete(id);
        return ResponseEntity.noContent().build();
    }
    //list all the patient under a doctor
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PROVIDER')")
    @GetMapping(value = "/fetch-patients-by-practitioner", produces = "application/fhir+json")
    public ResponseEntity<String>
    fetchPatientsByPractitioner(@RequestParam(required = false) UUID id,
                                @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return fhirResponseHelper.toResponse(practitionerService.fetchPatientsByPractitioner(id,pageable));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER')")
    @PostMapping(value = "/ai-recommendation-instructions", consumes = "application/json", produces = "application/fhir+json")
    public ResponseEntity<String> createAiRecommendationInstructions(@Valid @RequestBody AiRecommendationInstructionsRequest request) {
        Communication created = aiRecommendationInstructionsService.create(request);
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .body(json);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER')")
    @PostMapping(value = "/ai-recommended-action", consumes = "application/json", produces = "application/fhir+json")
    public ResponseEntity<String> createAiRecommendedAction(@Valid @RequestBody AiRecommendedActionRequest request) {
        Communication created = aiRecommendedActionService.create(request);
        String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .body(json);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'PROVIDER')")
    @GetMapping(value = "/risk-assignment", produces = "application/json")
    public ResponseEntity<ProviderRiskScoreResponse> getLatestRiskScore(
            @RequestParam UUID patientId,
            @RequestParam UUID orgId) {
        ProviderRiskScoreResponse response = practitionerService.getLatestRiskScore(patientId, orgId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'PROVIDER')")
    @PostMapping(value = "/risk-assignment", consumes = "application/json", produces = "application/fhir+json")
    public ResponseEntity<String> createRiskScoreByProvider(@Valid @RequestBody CreateP360RiskScoreRequest request) {
        RiskAssessment created = practitionerService.createRiskScore(request);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .header("Location", "/baseR4/P360RiskScore/" + created.getIdElement().getIdPart())
                .body(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created));
    }

}

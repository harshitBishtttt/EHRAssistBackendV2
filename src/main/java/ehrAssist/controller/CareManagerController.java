package ehrAssist.controller;

import ca.uhn.fhir.context.FhirContext;
import ehrAssist.dto.request.CreateCareCoordinationNoteRequest;
import ehrAssist.dto.request.CreateP360RiskScoreRequest;
import ehrAssist.service.CareManagerService;
import ehrAssist.util.FhirResponseHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.RiskAssessment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/baseR4/CareCoordinationNote")
@RequiredArgsConstructor
public class CareManagerController {

    private final CareManagerService careCoordinationNoteService;
    private final FhirResponseHelper fhirResponseHelper;
    private final FhirContext fhirContext;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER')")
    @GetMapping(value = "/search", produces = "application/fhir+json")
    public ResponseEntity<String> search(
            @RequestParam UUID patientId,
            @RequestParam String coordinatorEmail,
            @RequestParam UUID actionId,
            @RequestParam String status) {
        Bundle bundle = careCoordinationNoteService.search(patientId, coordinatorEmail, actionId, status);
        return fhirResponseHelper.toResponse(bundle);
    }

    @PostMapping(produces = "application/fhir+json")
    public ResponseEntity<String> create(@RequestBody CreateCareCoordinationNoteRequest request) {
        DocumentReference created = careCoordinationNoteService.create(request);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .header("Location", "/baseR4/CareCoordinationNote/" + created.getIdElement().getIdPart())
                .body(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created));
    }

    @PatchMapping
    public ResponseEntity<?> deactivateNotes(@RequestParam String email,
                                             @RequestParam UUID patientId,
                                             @RequestParam UUID actionId,
                                             @RequestParam String status) {
        careCoordinationNoteService.deactivateNotes(email, patientId, actionId, status);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .body("");
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER')")
    @PostMapping(value = "/risk-assignment", consumes = "application/json", produces = "application/fhir+json")
    public ResponseEntity<String> createRiskScoreByCareManager(@Valid @RequestBody CreateP360RiskScoreRequest request) {
        RiskAssessment created = careCoordinationNoteService.createRiskScore(request);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .header("Location", "/baseR4/P360RiskScore/" + created.getIdElement().getIdPart())
                .body(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created));
    }
}

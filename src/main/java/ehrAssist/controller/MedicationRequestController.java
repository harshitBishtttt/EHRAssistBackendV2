package ehrAssist.controller;

import ca.uhn.fhir.context.FhirContext;
import ehrAssist.service.MedicationRequestService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

@RestController
@RequestMapping("/baseR4/MedicationRequest")
@RequiredArgsConstructor
public class MedicationRequestController {

    private final MedicationRequestService medicationRequestService;
    private final FhirResponseHelper fhirResponseHelper;
    private final FhirContext fhirContext;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER', 'PATIENT')")
    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getById(@PathVariable UUID id) {
        MedicationRequest medicationRequest = medicationRequestService.getById(id);
        return fhirResponseHelper.toResponse(medicationRequest);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER', 'PATIENT')")
    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> search(
            @RequestParam(required = false) UUID _id,
            @RequestParam(required = false) UUID patient,
            @RequestParam(required = false) String status,
            @RequestParam(name = "formulary-drug-cd", required = false) String formularyDrugCd,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Bundle bundle = medicationRequestService.search(_id, patient, status, formularyDrugCd, pageable);
        return fhirResponseHelper.toResponse(bundle);
    }

    @PostMapping(consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> create(@RequestBody String body) {
        MedicationRequest medicationRequest = fhirContext.newJsonParser().parseResource(MedicationRequest.class, body);
        MedicationRequest created = medicationRequestService.create(medicationRequest);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .header("Location", "/baseR4/MedicationRequest/" + created.getIdElement().getIdPart())
                .body(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created));
    }

    @PutMapping(value = "/{id}", consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> update(@PathVariable UUID id, @RequestBody String body) {
        MedicationRequest medicationRequest = fhirContext.newJsonParser().parseResource(MedicationRequest.class, body);
        MedicationRequest updated = medicationRequestService.update(id, medicationRequest);
        return fhirResponseHelper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        medicationRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

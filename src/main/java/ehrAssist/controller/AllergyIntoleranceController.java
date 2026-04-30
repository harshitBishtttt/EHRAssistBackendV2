package ehrAssist.controller;

import ca.uhn.fhir.context.FhirContext;
import ehrAssist.service.AllergyIntoleranceService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

@RestController
@RequestMapping("/baseR4/AllergyIntolerance")
@RequiredArgsConstructor
public class AllergyIntoleranceController {

    private final AllergyIntoleranceService allergyIntoleranceService;
    private final FhirResponseHelper fhirResponseHelper;
    private final FhirContext fhirContext;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER', 'PATIENT')")
    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getById(@PathVariable UUID id) {
        AllergyIntolerance allergyIntolerance = allergyIntoleranceService.getById(id);
        return fhirResponseHelper.toResponse(allergyIntolerance);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER', 'PATIENT')")
    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> search(
            @RequestParam(required = false) UUID _id,
            @RequestParam(required = false) UUID patient,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Bundle bundle = allergyIntoleranceService.search(_id, patient, pageable);
        return fhirResponseHelper.toResponse(bundle);
    }

    @PostMapping(consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> create(@RequestBody String body) {
        AllergyIntolerance allergyIntolerance = fhirContext.newJsonParser().parseResource(AllergyIntolerance.class, body);
        AllergyIntolerance created = allergyIntoleranceService.create(allergyIntolerance);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .header("Location", "/baseR4/AllergyIntolerance/" + created.getIdElement().getIdPart())
                .body(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created));
    }

    @PutMapping(value = "/{id}", consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> update(@PathVariable UUID id, @RequestBody String body) {
        AllergyIntolerance allergyIntolerance = fhirContext.newJsonParser().parseResource(AllergyIntolerance.class, body);
        AllergyIntolerance updated = allergyIntoleranceService.update(id, allergyIntolerance);
        return fhirResponseHelper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        allergyIntoleranceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

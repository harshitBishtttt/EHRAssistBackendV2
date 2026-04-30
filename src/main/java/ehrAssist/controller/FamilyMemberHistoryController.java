package ehrAssist.controller;

import ca.uhn.fhir.context.FhirContext;
import ehrAssist.service.FamilyMemberHistoryService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.FamilyMemberHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

@RestController
@RequestMapping("/baseR4/FamilyMemberHistory")
@RequiredArgsConstructor
public class FamilyMemberHistoryController {

    private final FamilyMemberHistoryService familyMemberHistoryService;
    private final FhirResponseHelper fhirResponseHelper;
    private final FhirContext fhirContext;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER', 'PATIENT')")
    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getById(@PathVariable UUID id) {
        FamilyMemberHistory fmh = familyMemberHistoryService.getById(id);
        return fhirResponseHelper.toResponse(fmh);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER', 'PATIENT')")
    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> search(
            @RequestParam(required = false) UUID _id,
            @RequestParam(required = false) UUID patient,
            @RequestParam(required = false) String relationship,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Bundle bundle = familyMemberHistoryService.search(_id, patient, relationship, pageable);
        return fhirResponseHelper.toResponse(bundle);
    }

    @PostMapping(consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> create(@RequestBody String body) {
        FamilyMemberHistory fmh = fhirContext.newJsonParser().parseResource(FamilyMemberHistory.class, body);
        FamilyMemberHistory created = familyMemberHistoryService.create(fmh);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .header("Location", "/baseR4/FamilyMemberHistory/" + created.getIdElement().getIdPart())
                .body(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created));
    }

    @PutMapping(value = "/{id}", consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> update(@PathVariable UUID id, @RequestBody String body) {
        FamilyMemberHistory fmh = fhirContext.newJsonParser().parseResource(FamilyMemberHistory.class, body);
        FamilyMemberHistory updated = familyMemberHistoryService.update(id, fmh);
        return fhirResponseHelper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        familyMemberHistoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

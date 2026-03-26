package ehrAssist.controller;

import ca.uhn.fhir.context.FhirContext;
import ehrAssist.service.ImmunizationService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Immunization;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/baseR4/Immunization")
@RequiredArgsConstructor
public class ImmunizationController {

    private final ImmunizationService immunizationService;
    private final FhirResponseHelper fhirResponseHelper;
    private final FhirContext fhirContext;

    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getById(@PathVariable UUID id) {
        Immunization immunization = immunizationService.getById(id);
        return fhirResponseHelper.toResponse(immunization);
    }

    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> search(
            @RequestParam(required = false) UUID _id,
            @RequestParam(required = false) UUID patient,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Bundle bundle = immunizationService.search(_id, patient, pageable);
        return fhirResponseHelper.toResponse(bundle);
    }

    @PostMapping(consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> create(@RequestBody String body) {
        Immunization immunization = fhirContext.newJsonParser().parseResource(Immunization.class, body);
        Immunization created = immunizationService.create(immunization);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .header("Location", "/baseR4/Immunization/" + created.getIdElement().getIdPart())
                .body(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created));
    }

    @PutMapping(value = "/{id}", consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> update(@PathVariable UUID id, @RequestBody String body) {
        Immunization immunization = fhirContext.newJsonParser().parseResource(Immunization.class, body);
        Immunization updated = immunizationService.update(id, immunization);
        return fhirResponseHelper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        immunizationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

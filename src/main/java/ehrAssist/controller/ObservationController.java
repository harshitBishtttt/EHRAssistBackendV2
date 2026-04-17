package ehrAssist.controller;

import ca.uhn.fhir.context.FhirContext;
import ehrAssist.service.ObservationService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/baseR4/Observation")
@RequiredArgsConstructor
public class ObservationController {

    private final ObservationService observationService;
    private final FhirResponseHelper fhirResponseHelper;
    private final FhirContext fhirContext;

    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getById(@PathVariable UUID id) {
        Observation observation = observationService.getById(id);
        return fhirResponseHelper.toResponse(observation);
    }

    @GetMapping(value = "/search", produces = "application/fhir+json")
    public ResponseEntity<String> search(
            @RequestParam(required = false) UUID _id,
            @RequestParam(required = false) UUID patient,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String category,
            @RequestParam(name = "value-quantity", required = false) String valueQuantity,
            @RequestParam(required = false) List<String> date,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Bundle bundle = observationService.search(_id, patient, code, category, valueQuantity, date, pageable);
        return fhirResponseHelper.toResponse(bundle);
    }

    @PostMapping(consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> create(@RequestBody String body) {
        Observation observation = fhirContext.newJsonParser().parseResource(Observation.class, body);
        Observation created = observationService.create(observation);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .header("Location", "/baseR4/Observation/" + created.getIdElement().getIdPart())
                .body(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created));
    }

    @PutMapping(value = "/{id}", consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> update(@PathVariable UUID id, @RequestBody String body) {
        Observation observation = fhirContext.newJsonParser().parseResource(Observation.class, body);
        Observation updated = observationService.update(id, observation);
        return fhirResponseHelper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        observationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/vitals/search", produces = "application/fhir+json")
    public ResponseEntity<String> searchVitals(
            @RequestParam(required = false) UUID patient,
            @RequestParam(required = false) String code,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Bundle bundle = observationService.searchVitals(patient, code, pageable);
        return fhirResponseHelper.toResponse(bundle);
    }
}

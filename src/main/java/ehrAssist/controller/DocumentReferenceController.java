package ehrAssist.controller;

import ca.uhn.fhir.context.FhirContext;
import ehrAssist.service.DocumentReferenceService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/baseR4/DocumentReference")
@RequiredArgsConstructor
public class DocumentReferenceController {

    private final DocumentReferenceService documentReferenceService;
    private final FhirResponseHelper fhirResponseHelper;
    private final FhirContext fhirContext;

    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getById(@PathVariable UUID id) {
        DocumentReference documentReference = documentReferenceService.getById(id);
        return fhirResponseHelper.toResponse(documentReference);
    }

    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> search(
            @RequestParam(required = false) UUID _id,
            @RequestParam(required = false) UUID patient,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Bundle bundle = documentReferenceService.search(_id, patient, pageable);
        return fhirResponseHelper.toResponse(bundle);
    }

    @PostMapping(consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> create(@RequestBody String body) {
        DocumentReference documentReference = fhirContext.newJsonParser().parseResource(DocumentReference.class, body);
        DocumentReference created = documentReferenceService.create(documentReference);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .header("Location", "/baseR4/DocumentReference/" + created.getIdElement().getIdPart())
                .body(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created));
    }

    @PutMapping(value = "/{id}", consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> update(@PathVariable UUID id, @RequestBody String body) {
        DocumentReference documentReference = fhirContext.newJsonParser().parseResource(DocumentReference.class, body);
        DocumentReference updated = documentReferenceService.update(id, documentReference);
        return fhirResponseHelper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentReferenceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

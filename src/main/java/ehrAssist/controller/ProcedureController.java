package ehrAssist.controller;

import ca.uhn.fhir.context.FhirContext;
import ehrAssist.service.ProcedureService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Procedure;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/baseR4/Procedure")
@RequiredArgsConstructor
public class ProcedureController {

    private final ProcedureService procedureService;
    private final FhirResponseHelper fhirResponseHelper;
    private final FhirContext fhirContext;

    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getById(@PathVariable UUID id) {
        Procedure procedure = procedureService.getById(id);
        return fhirResponseHelper.toResponse(procedure);
    }

    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> search(
            @RequestParam(required = false) UUID _id,
            @RequestParam(required = false) UUID patient,
            @RequestParam(required = false) Integer code,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Bundle bundle = procedureService.search(_id, patient, code, pageable);
        return fhirResponseHelper.toResponse(bundle);
    }

    @PostMapping(consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> create(@RequestBody String body) {
        Procedure procedure = fhirContext.newJsonParser().parseResource(Procedure.class, body);
        Procedure created = procedureService.create(procedure);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .header("Location", "/baseR4/Procedure/" + created.getIdElement().getIdPart())
                .body(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created));
    }

    @PutMapping(value = "/{id}", consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> update(@PathVariable UUID id, @RequestBody String body) {
        Procedure procedure = fhirContext.newJsonParser().parseResource(Procedure.class, body);
        Procedure updated = procedureService.update(id, procedure);
        return fhirResponseHelper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        procedureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

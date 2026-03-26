package ehrAssist.controller;

import ca.uhn.fhir.context.FhirContext;
import ehrAssist.service.DiagnosticReportService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/baseR4/DiagnosticReport")
@RequiredArgsConstructor
public class DiagnosticReportController {

    private final DiagnosticReportService diagnosticReportService;
    private final FhirResponseHelper fhirResponseHelper;
    private final FhirContext fhirContext;

    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getById(@PathVariable UUID id) {
        DiagnosticReport diagnosticReport = diagnosticReportService.getById(id);
        return fhirResponseHelper.toResponse(diagnosticReport);
    }

    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> search(
            @RequestParam(required = false) UUID _id,
            @RequestParam(required = false) UUID patient,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Bundle bundle = diagnosticReportService.search(_id, patient, pageable);
        return fhirResponseHelper.toResponse(bundle);
    }

    @PostMapping(consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> create(@RequestBody String body) {
        DiagnosticReport diagnosticReport = fhirContext.newJsonParser().parseResource(DiagnosticReport.class, body);
        DiagnosticReport created = diagnosticReportService.create(diagnosticReport);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .header("Location", "/baseR4/DiagnosticReport/" + created.getIdElement().getIdPart())
                .body(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created));
    }

    @PutMapping(value = "/{id}", consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> update(@PathVariable UUID id, @RequestBody String body) {
        DiagnosticReport diagnosticReport = fhirContext.newJsonParser().parseResource(DiagnosticReport.class, body);
        DiagnosticReport updated = diagnosticReportService.update(id, diagnosticReport);
        return fhirResponseHelper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        diagnosticReportService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

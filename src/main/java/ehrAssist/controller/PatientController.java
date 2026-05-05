package ehrAssist.controller;

import ca.uhn.fhir.context.FhirContext;
import ehrAssist.service.PatientService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/baseR4/Patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;
    private final FhirResponseHelper fhirResponseHelper;
    private final FhirContext fhirContext;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER', 'PATIENT')")
    @GetMapping(value = "/find", produces = "application/fhir+json")
    public ResponseEntity<String> getById(@RequestParam UUID id) {
        Patient patient = patientService.getById(id);
        return fhirResponseHelper.toResponse(patient);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER')")
    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> search(
            @RequestParam(required = false) UUID _id,
            @RequestParam(required = false) String family,
            @RequestParam(required = false) String given,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String birthdate,
            @RequestParam(required = false) String email,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Bundle bundle = patientService.search(_id, family, given, gender, birthdate, email, pageable);
        return fhirResponseHelper.toResponse(bundle);
    }

    @PostMapping(consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> create(@RequestBody String body) {
        Patient patient = fhirContext.newJsonParser().parseResource(Patient.class, body);
        Patient created = patientService.create(patient);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .header("Location", "/baseR4/Patient/" + created.getIdElement().getIdPart())
                .body(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(created));
    }

    @PutMapping(value = "/{id}", consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json")
    public ResponseEntity<String> update(@PathVariable UUID id, @RequestBody String body) {
        Patient patient = fhirContext.newJsonParser().parseResource(Patient.class, body);
        Patient updated = patientService.update(id, patient);
        return fhirResponseHelper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        patientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

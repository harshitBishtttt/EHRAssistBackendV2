package ehrAssist.controller;

import ca.uhn.fhir.context.FhirContext;
import ehrAssist.dto.request.CreateCareCoordinationNoteRequest;
import ehrAssist.service.CareCoordinationNoteService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/baseR4/CareCoordinationNote")
@RequiredArgsConstructor
public class CareCoordinationNoteController {

    private final CareCoordinationNoteService careCoordinationNoteService;
    private final FhirResponseHelper fhirResponseHelper;
    private final FhirContext fhirContext;

    @GetMapping(value = "/search", produces = "application/fhir+json")
    public ResponseEntity<String> search(
            @RequestParam UUID patientId,
            @RequestParam String coordinatorEmail) {
        Bundle bundle = careCoordinationNoteService.search(patientId, coordinatorEmail);
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
}

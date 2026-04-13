package ehrAssist.controller;

import ehrAssist.service.CareCoordinationNoteService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping(value = "/search", produces = "application/fhir+json")
    public ResponseEntity<String> search(
            @RequestParam UUID patientId,
            @RequestParam String coordinatorEmail) {
        Bundle bundle = careCoordinationNoteService.search(patientId, coordinatorEmail);
        return fhirResponseHelper.toResponse(bundle);
    }
}

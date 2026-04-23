package ehrAssist.controller;

import ehrAssist.service.CareGapService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/baseR4/Measure")
@RequiredArgsConstructor
public class MeasureController {

    private final CareGapService careGapService;
    private final FhirResponseHelper fhirResponseHelper;

    @GetMapping(value = "/$care-gaps", produces = "application/fhir+json")
    public ResponseEntity<String> getCareGaps(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String practitioner,
            @RequestParam(required = false) String organization,
            @RequestParam(defaultValue = "open-gap") String status) {

        Bundle bundle = careGapService.evaluateDiabetesCareGaps(
                periodStart,
                periodEnd,
                subject,
                practitioner,
                organization,
                status
        );
        return fhirResponseHelper.toResponse(bundle);
    }
}

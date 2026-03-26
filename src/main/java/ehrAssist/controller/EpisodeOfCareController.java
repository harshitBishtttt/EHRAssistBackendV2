package ehrAssist.controller;

import ehrAssist.service.EpisodeOfCareService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/baseR4/EpisodeOfCare")
@RequiredArgsConstructor
public class EpisodeOfCareController {

    private final EpisodeOfCareService episodeOfCareService;
    private final FhirResponseHelper fhirResponseHelper;

    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getById(@PathVariable UUID id) {
        EpisodeOfCare resource = episodeOfCareService.getById(id);
        return fhirResponseHelper.toResponse(resource);
    }

    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> search(
            @RequestParam(required = false) UUID _id,
            @RequestParam(required = false) UUID patient,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Bundle bundle = episodeOfCareService.search(_id, patient, status, type, pageable);
        return fhirResponseHelper.toResponse(bundle);
    }
}

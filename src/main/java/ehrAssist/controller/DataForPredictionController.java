package ehrAssist.controller;

import ehrAssist.service.DataForPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/baseR4/Patient")
@RequiredArgsConstructor
public class DataForPredictionController {

    private final DataForPredictionService dataForPredictionService;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER')")
    @GetMapping(value = "/{patientId}/prediction-data", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getPredictionData(@PathVariable UUID patientId) {
        Map<String, Object> result = dataForPredictionService.buildPredictionData(patientId);
        return ResponseEntity.ok(result);
    }
}

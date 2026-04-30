package ehrAssist.controller;

import ehrAssist.service.RiskInsightsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/predict")
public class RiskInsightsController {

    private final RiskInsightsService riskInsightsService;

    public RiskInsightsController(RiskInsightsService riskInsightsService) {
        this.riskInsightsService = riskInsightsService;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER')")
    @GetMapping("/risk-insights")
    ResponseEntity<String> getCashedRiskInsights(@RequestParam UUID patient_id,
                                                 @RequestHeader("Authorization") String authHeader) {
        String html = riskInsightsService.getCashedRiskInsights(patient_id, authHeader);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

}

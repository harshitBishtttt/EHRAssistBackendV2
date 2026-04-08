package ehrAssist.service.impl;

import ehrAssist.entity.RiskInsightsCashingEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.repository.ConditionRepository;
import ehrAssist.repository.ObservationRepository;
import ehrAssist.repository.RiskInsightsRepository;
import ehrAssist.service.RiskInsightsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class RiskInsightsServiceImpl implements RiskInsightsService {

    private final ObservationRepository observationRepository;
    private final ConditionRepository conditionRepository;
    private final RiskInsightsRepository riskInsightsRepository;
    private final RestTemplate restTemplate;

    @Value("${risk-insights.predict-url:https://fhirassist.rsystems.com:5050/api/predict}")
    private String predictApiUrl;

    private RiskInsightsServiceImpl(ObservationRepository observationRepository,
                                    ConditionRepository conditionRepository,
                                    RiskInsightsRepository riskInsightsRepository,
                                    RestTemplate restTemplate) {
        this.observationRepository = observationRepository;
        this.conditionRepository = conditionRepository;
        this.riskInsightsRepository = riskInsightsRepository;
        this.restTemplate = restTemplate;
    }

    public String getCashedRiskInsights(UUID patientId, String authHeader) {
        Optional<LocalDateTime> latestObservationOfPatient = observationRepository
                .findLatestIssuedDateByPatientId(patientId);
        Optional<LocalDate> latestConditionOfPatient = conditionRepository
                .findLatestRecordedDateByPatientId(patientId);

        if (latestObservationOfPatient.isPresent() && latestConditionOfPatient.isPresent()) {
            Optional<String> reportHtml = riskInsightsRepository
                    .findReportHtml(patientId, latestObservationOfPatient.get(), latestConditionOfPatient.get());

            if (reportHtml.isPresent()) {
                return reportHtml.get();
            } else {
                String html = callPredictApi(patientId, authHeader);

                RiskInsightsCashingEntity entity = new RiskInsightsCashingEntity();
                entity.setPatientId(patientId);
                entity.setLastObservationDate(latestObservationOfPatient.get());
                entity.setLastConditionDate(latestConditionOfPatient.get());
                entity.setReportHtml(html);
                entity.setCreatedAt(LocalDateTime.now());
                riskInsightsRepository.save(entity);

                return html;
            }
        } else {
            throw new ResourceNotFoundException(
                    "No observations or conditions found on record for patient: " + patientId);
        }
    }

    private String callPredictApi(UUID patientId, String authHeader) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", authHeader);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(
                Map.of("uuid", patientId.toString()), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                predictApiUrl, HttpMethod.POST, request, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        throw new RuntimeException("Predict API failed with status: " + response.getStatusCode());
    }
}

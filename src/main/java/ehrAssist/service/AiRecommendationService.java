package ehrAssist.service;

import ehrAssist.dto.request.AiRecommendationRequest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Communication;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AiRecommendationService {

    Communication create(AiRecommendationRequest request);

    Bundle getByPatientId(UUID patientId, Pageable pageable);
}

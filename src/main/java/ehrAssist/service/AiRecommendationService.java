package ehrAssist.service;

import ehrAssist.dto.request.AiRecommendationRequest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Communication;

import java.util.UUID;

public interface AiRecommendationService {

    Communication create(AiRecommendationRequest request);

    Bundle getVerifiedByPatientId(UUID patientId);
}

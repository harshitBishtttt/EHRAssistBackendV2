package ehrAssist.service;

import ehrAssist.dto.request.AiRecommendationRequest;
import org.hl7.fhir.r4.model.Communication;

public interface AiRecommendationService {

    Communication create(AiRecommendationRequest request);
}

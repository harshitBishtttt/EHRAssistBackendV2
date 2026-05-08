package ehrAssist.service;

import ehrAssist.dto.request.AiRecommendationInstructionsRequest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Communication;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AiRecommendationInstructionsService {

    Communication create(AiRecommendationInstructionsRequest request);

    Bundle getByPatientId(UUID patientId, Pageable pageable);
}

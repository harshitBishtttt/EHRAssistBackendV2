package ehrAssist.service;

import ehrAssist.dto.request.AiRecommendedActionRequest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Communication;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AiRecommendedActionService {

    Communication create(AiRecommendedActionRequest request);

    Bundle getByPatientId(UUID patientId, Pageable pageable);
}

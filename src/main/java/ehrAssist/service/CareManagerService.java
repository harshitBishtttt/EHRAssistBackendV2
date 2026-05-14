package ehrAssist.service;

import ehrAssist.dto.request.CreateCareCoordinationNoteRequest;
import ehrAssist.dto.request.CreateP360RiskScoreRequest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.RiskAssessment;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CareManagerService {
    Bundle search(UUID patientId, String coordinatorEmail, UUID actionId, String status);

    DocumentReference create(CreateCareCoordinationNoteRequest request);

    void deactivateNotes(String email, UUID patientId, UUID actionId, String status);


    RiskAssessment createRiskScore(CreateP360RiskScoreRequest request);
}

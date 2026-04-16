package ehrAssist.service;

import ehrAssist.dto.request.CreateCareCoordinationNoteRequest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;

import java.util.UUID;

public interface CareCoordinationNoteService {
    Bundle search(UUID patientId, String coordinatorEmail, UUID actionId, String status);

    DocumentReference create(CreateCareCoordinationNoteRequest request);

    void deactivateNotes(String email, UUID patientId, UUID actionId, String status);
}

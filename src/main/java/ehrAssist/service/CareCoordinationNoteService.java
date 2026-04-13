package ehrAssist.service;

import ehrAssist.dto.request.CreateCareCoordinationNoteRequest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;

import java.util.UUID;

public interface CareCoordinationNoteService {
    Bundle search(UUID patientId, String coordinatorEmail);
    DocumentReference create(CreateCareCoordinationNoteRequest request);
}

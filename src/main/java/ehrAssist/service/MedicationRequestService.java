package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MedicationRequestService {
    MedicationRequest getById(UUID id);
    Bundle search(UUID id, UUID patientId, String status, String formularyDrugCd, Pageable pageable);
    MedicationRequest create(MedicationRequest resource);
    MedicationRequest update(UUID id, MedicationRequest resource);
    void delete(UUID id);
}

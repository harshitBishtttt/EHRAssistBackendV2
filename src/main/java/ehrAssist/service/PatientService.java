package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PatientService {
    Patient getById(UUID id);
    Bundle search(UUID id, String family, String given, String gender, String birthdate, String email, Pageable pageable);
    Patient create(Patient resource);
    Patient update(UUID id, Patient resource);
    void delete(UUID id);
}

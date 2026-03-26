package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Immunization;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ImmunizationService {
    Immunization getById(UUID id);
    Bundle search(UUID id, UUID patientId, Pageable pageable);
    Immunization create(Immunization resource);
    Immunization update(UUID id, Immunization resource);
    void delete(UUID id);
}

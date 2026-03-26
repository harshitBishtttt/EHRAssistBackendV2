package ehrAssist.service;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AllergyIntoleranceService {
    AllergyIntolerance getById(UUID id);
    Bundle search(UUID id, UUID patientId, Pageable pageable);
    AllergyIntolerance create(AllergyIntolerance resource);
    AllergyIntolerance update(UUID id, AllergyIntolerance resource);
    void delete(UUID id);
}

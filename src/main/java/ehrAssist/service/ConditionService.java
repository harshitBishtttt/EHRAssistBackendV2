package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ConditionService {
    Condition getById(UUID id);
    Bundle search(UUID id, UUID patientId, String code, Pageable pageable);
    Condition create(Condition resource);
    Condition update(UUID id, Condition resource);
    void delete(UUID id);
}

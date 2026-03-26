package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Procedure;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProcedureService {
    Procedure getById(UUID id);
    Bundle search(UUID id, UUID patientId, Integer code, Pageable pageable);
    Procedure create(Procedure resource);
    Procedure update(UUID id, Procedure resource);
    void delete(UUID id);
}

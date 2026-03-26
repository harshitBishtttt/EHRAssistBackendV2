package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PractitionerService {
    Practitioner getById(UUID id);
    Bundle search(UUID id, String name, String specialty, Pageable pageable);
    Practitioner create(Practitioner resource);
    Practitioner update(UUID id, Practitioner resource);
    void delete(UUID id);
}

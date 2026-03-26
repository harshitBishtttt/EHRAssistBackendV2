package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface EncounterService {
    Encounter getById(UUID id);
    Bundle search(UUID id, UUID patientId, String status, String encounterClass, List<String> date, Pageable pageable);
    Encounter create(Encounter resource);
    Encounter update(UUID id, Encounter resource);
    void delete(UUID id);
}

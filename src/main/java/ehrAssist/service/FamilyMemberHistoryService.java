package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.FamilyMemberHistory;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FamilyMemberHistoryService {
    FamilyMemberHistory getById(UUID id);
    Bundle search(UUID id, UUID patientId, String relationship, Pageable pageable);
    FamilyMemberHistory create(FamilyMemberHistory resource);
    FamilyMemberHistory update(UUID id, FamilyMemberHistory resource);
    void delete(UUID id);
}

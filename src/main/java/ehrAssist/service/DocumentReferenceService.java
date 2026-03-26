package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DocumentReferenceService {
    DocumentReference getById(UUID id);
    Bundle search(UUID id, UUID patientId, Pageable pageable);
    DocumentReference create(DocumentReference resource);
    DocumentReference update(UUID id, DocumentReference resource);
    void delete(UUID id);
}

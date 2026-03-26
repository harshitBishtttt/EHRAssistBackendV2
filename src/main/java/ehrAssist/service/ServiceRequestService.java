package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ServiceRequestService {
    ServiceRequest getById(UUID id);
    Bundle search(UUID id, UUID patientId, Pageable pageable);
    ServiceRequest create(ServiceRequest resource);
    ServiceRequest update(UUID id, ServiceRequest resource);
    void delete(UUID id);
}

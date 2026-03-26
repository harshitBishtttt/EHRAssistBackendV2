package ehrAssist.service;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AppointmentService {
    Appointment getById(UUID id);
    Bundle search(UUID id, UUID patientId, String status, Pageable pageable);
    Appointment create(Appointment resource);
    Appointment update(UUID id, Appointment resource);
    void delete(UUID id);
}

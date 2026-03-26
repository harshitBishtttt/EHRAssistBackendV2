package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DiagnosticReportService {
    DiagnosticReport getById(UUID id);
    Bundle search(UUID id, UUID patientId, Pageable pageable);
    DiagnosticReport create(DiagnosticReport resource);
    DiagnosticReport update(UUID id, DiagnosticReport resource);
    void delete(UUID id);
}

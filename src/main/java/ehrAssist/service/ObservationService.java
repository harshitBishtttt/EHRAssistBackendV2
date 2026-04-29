package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ObservationService {
    Observation getById(UUID id);
    Bundle search(UUID id, UUID patientId, String code, String category, String valueQuantity, List<String> date,
                    Pageable pageable);
    Observation create(Observation resource);
    Observation update(UUID id, Observation resource);
    void delete(UUID id);

    Bundle searchVitals(UUID patientId, String loincCode, Pageable pageable);

    Bundle getRiskFeed(UUID practitionerId, int rank);
}

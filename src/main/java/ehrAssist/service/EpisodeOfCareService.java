package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EpisodeOfCareService {

    EpisodeOfCare getById(UUID id);

    Bundle search(UUID id, UUID patientId, String status, String type, Pageable pageable);
}

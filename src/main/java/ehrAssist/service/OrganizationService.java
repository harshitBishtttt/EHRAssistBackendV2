package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrganizationService {
    Bundle getOrganizationsByCareManager(UUID careManagerId, Pageable pageable);
}

package ehrAssist.service;

import ehrAssist.dto.response.PractitionerDropdownResponse;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PractitionerService {
    Practitioner getById(UUID id);
    Bundle search(UUID id, String name, String specialty, Pageable pageable);
    List<PractitionerDropdownResponse> listPractitionerDropdown();
    Practitioner create(Practitioner resource);
    Practitioner update(UUID id, Practitioner resource);
    void delete(UUID id);
}

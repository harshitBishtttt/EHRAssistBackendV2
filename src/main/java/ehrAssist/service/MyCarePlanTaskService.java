package ehrAssist.service;

import org.hl7.fhir.r4.model.Bundle;

import java.util.UUID;

public interface MyCarePlanTaskService {
    Bundle getCarePlanTasks(UUID patientId);
}

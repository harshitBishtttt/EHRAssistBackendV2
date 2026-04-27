package ehrAssist.service;

import org.hl7.fhir.r4.model.CarePlan;

import java.util.UUID;

public interface LifestyleGoalService {
    CarePlan getLifestyleGoals(UUID patientId);
}

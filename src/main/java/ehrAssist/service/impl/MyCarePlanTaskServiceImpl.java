package ehrAssist.service.impl;

import ehrAssist.entity.CareGoalEntity;
import ehrAssist.exception.FhirValidationException;
import ehrAssist.repository.CareGoalRepository;
import ehrAssist.service.MyCarePlanTaskService;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Goal;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MyCarePlanTaskServiceImpl implements MyCarePlanTaskService {

    private final CareGoalRepository careGoalRepository;

    @Override
    @Transactional(readOnly = true)
    public Bundle getCarePlanTasks(UUID patientId) {
        if (patientId == null) {
            throw new FhirValidationException("patientId is required.");
        }

        // Single query: active tasks (exclude completed/cancelled)
        List<CareGoalEntity> goalEntities = careGoalRepository
                .findByPatientIdAndStatusNotInOrderByCreatedAtAsc(patientId, List.of("completed", "cancelled"));

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTimestamp(new Date());

        goalEntities.stream()
                .map(entity -> buildGoalResource(entity, patientId))
                .forEach(goal -> bundle.addEntry()
                        .setFullUrl(goal.getId())
                        .setResource(goal));

        bundle.setTotal(bundle.getEntry().size());
        return bundle;
    }

    private Goal buildGoalResource(CareGoalEntity entity, UUID patientId) {
        Goal goal = new Goal();
        goal.setId("Goal/" + entity.getId());
        goal.setLifecycleStatus(toGoalLifecycleStatus(entity.getStatus()));
        goal.setDescription(new CodeableConcept().setText(entity.getDisplay()));
        goal.setSubject(new Reference("Patient/" + patientId));
        goal.addIdentifier(new Identifier()
                .setSystem("http://ehrassist.com/fhir/care-goal")
                .setValue(entity.getId().toString()));

        if (entity.getDescription() != null && !entity.getDescription().isBlank()) {
            goal.addNote(new Annotation().setText(entity.getDescription()));
        }
        if (entity.getKind() != null && !entity.getKind().isBlank()) {
            goal.addCategory(new CodeableConcept().setText(entity.getKind()));
        }
        return goal;
    }

    private Goal.GoalLifecycleStatus toGoalLifecycleStatus(String status) {
        return switch (normalize(status)) {
            case "completed"            -> Goal.GoalLifecycleStatus.COMPLETED;
            case "cancelled"            -> Goal.GoalLifecycleStatus.CANCELLED;
            case "on-hold"              -> Goal.GoalLifecycleStatus.ONHOLD;
            case "not-started","proposed" -> Goal.GoalLifecycleStatus.PROPOSED;
            default                     -> Goal.GoalLifecycleStatus.ACTIVE;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

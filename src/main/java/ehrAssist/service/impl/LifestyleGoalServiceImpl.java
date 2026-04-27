package ehrAssist.service.impl;

import ehrAssist.dto.projection.LifestyleGoalSummaryProjection;
import ehrAssist.exception.FhirValidationException;
import ehrAssist.repository.LifestyleGoalDailyRepository;
import ehrAssist.service.LifestyleGoalService;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LifestyleGoalServiceImpl implements LifestyleGoalService {

    private static final int TARGET_STEPS            = 10000;
    private static final int TARGET_WATER_GLASSES    = 8;
    private static final int TARGET_EXERCISE_MINUTES = 30;

    private static final String LIFESTYLE_GOALS_EXT = "http://ehrassist.com/fhir/StructureDefinition/lifestyle-goals";
    private static final String WEEKLY_PCT_EXT      = "http://ehrassist.com/fhir/StructureDefinition/weekly-completion-pct";

    private final LifestyleGoalDailyRepository lifestyleGoalDailyRepository;

    @Override
    @Transactional(readOnly = true)
    public CarePlan getLifestyleGoals(UUID patientId) {
        if (patientId == null) {
            throw new FhirValidationException("patientId is required.");
        }

        // Query 1: today's SUM from DB
        LifestyleGoalSummaryProjection today = lifestyleGoalDailyRepository.findTodayTotals(patientId);

        // Query 2: last 7 days SUM from DB for weekly % ring
        LifestyleGoalSummaryProjection weekly = lifestyleGoalDailyRepository.findWeeklyTotals(patientId);

        return buildCarePlan(patientId, today, weekly);
    }

    private CarePlan buildCarePlan(UUID patientId,
                                   LifestyleGoalSummaryProjection today,
                                   LifestyleGoalSummaryProjection weekly) {
        CarePlan carePlan = new CarePlan();
        carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
        carePlan.setIntent(CarePlan.CarePlanIntent.PLAN);
        carePlan.setSubject(new Reference("Patient/" + patientId));
        carePlan.setTitle("Lifestyle Goals");
        carePlan.addIdentifier(new Identifier()
                .setSystem("http://ehrassist.com/fhir/lifestyle-goals")
                .setValue(patientId.toString()));
        carePlan.addExtension(buildLifestyleExtension(today, weekly));
        return carePlan;
    }

    private Extension buildLifestyleExtension(LifestyleGoalSummaryProjection today,
                                               LifestyleGoalSummaryProjection weekly) {
        int achievedSteps    = safe(today.getAchievedSteps());
        int achievedWater    = safe(today.getAchievedWaterGlasses());
        int achievedExercise = safe(today.getAchievedExerciseMinutes());
        double weeklyPct     = computeWeeklyPct(weekly);

        Extension root = new Extension(LIFESTYLE_GOALS_EXT);

        // Daily Steps
        Extension stepsExt = new Extension("dailySteps");
        stepsExt.addExtension(new Extension("achieved", new IntegerType(achievedSteps)));
        stepsExt.addExtension(new Extension("target",   new IntegerType(TARGET_STEPS)));
        root.addExtension(stepsExt);

        // Water Intake
        Extension waterExt = new Extension("waterIntake");
        waterExt.addExtension(new Extension("achieved", new IntegerType(achievedWater)));
        waterExt.addExtension(new Extension("target",   new IntegerType(TARGET_WATER_GLASSES)));
        root.addExtension(waterExt);

        // Exercise Minutes
        Extension exerciseExt = new Extension("exerciseMinutes");
        exerciseExt.addExtension(new Extension("achieved", new IntegerType(achievedExercise)));
        exerciseExt.addExtension(new Extension("target",   new IntegerType(TARGET_EXERCISE_MINUTES)));
        root.addExtension(exerciseExt);

        // Weekly completion % — backend computed
        root.addExtension(new Extension(WEEKLY_PCT_EXT, new DecimalType(weeklyPct)));

        // Today's date
        root.addExtension(new Extension("date", new DateType(LocalDate.now().toString())));

        return root;
    }

    /**
     * Weekly % = average of (steps%, water%, exercise%) across last 7 days.
     * SUM(achieved) / (target × 7) × 100, averaged over 3 metrics.
     */
    private double computeWeeklyPct(LifestyleGoalSummaryProjection weekly) {
        double stepsPct    = (double) safe(weekly.getAchievedSteps())           / (TARGET_STEPS            * 7) * 100.0;
        double waterPct    = (double) safe(weekly.getAchievedWaterGlasses())    / (TARGET_WATER_GLASSES    * 7) * 100.0;
        double exercisePct = (double) safe(weekly.getAchievedExerciseMinutes()) / (TARGET_EXERCISE_MINUTES * 7) * 100.0;
        double avg = (stepsPct + waterPct + exercisePct) / 3.0;
        return Math.min(100.0, Math.round(avg * 10.0) / 10.0);
    }

    private int safe(Integer value) {
        return value != null ? value : 0;
    }
}

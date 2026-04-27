package ehrAssist.repository;

import ehrAssist.dto.projection.LifestyleGoalSummaryProjection;
import ehrAssist.entity.LifestyleGoalDailyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LifestyleGoalDailyRepository extends JpaRepository<LifestyleGoalDailyEntity, UUID> {

    // SUM for the current UTC day (00:00:00 → 23:59:59)
    @Query(value = """
            SELECT
                SUM(steps)                AS achievedSteps,
                SUM(water_intake_glasses) AS achievedWaterGlasses,
                SUM(exercise_minutes)     AS achievedExerciseMinutes
            FROM dbo.lifestyle_goal
            WHERE patient_id = :patientId
              AND CAST(created_at AS DATE) = CAST(GETUTCDATE() AS DATE)
            """, nativeQuery = true)
    LifestyleGoalSummaryProjection findTodayTotals(@Param("patientId") UUID patientId);

    // SUM for last 7 days (today − 6 days → today) for weekly % ring
    @Query(value = """
            SELECT
                SUM(steps)                AS achievedSteps,
                SUM(water_intake_glasses) AS achievedWaterGlasses,
                SUM(exercise_minutes)     AS achievedExerciseMinutes
            FROM dbo.lifestyle_goal
            WHERE patient_id = :patientId
              AND CAST(created_at AS DATE) >= CAST(DATEADD(DAY, -6, GETUTCDATE()) AS DATE)
              AND CAST(created_at AS DATE) <= CAST(GETUTCDATE() AS DATE)
            """, nativeQuery = true)
    LifestyleGoalSummaryProjection findWeeklyTotals(@Param("patientId") UUID patientId);
}

package ehrAssist.repository;

import ehrAssist.entity.RiskInsightsCashingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskInsightsRepository extends JpaRepository<RiskInsightsCashingEntity, UUID> {
    @Query(value = "SELECT r.report_html FROM risk_insights_cashing r WHERE r.patient_id = :patientId AND " +
            "r.last_observation_date = :lastObservationDate AND r.last_condition_date = :lastConditionDate",
            nativeQuery = true)
    Optional<String> findReportHtml(UUID patientId, LocalDateTime lastObservationDate, LocalDate lastConditionDate);

}

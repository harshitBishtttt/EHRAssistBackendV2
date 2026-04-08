package ehrAssist.repository;

import ehrAssist.entity.ConditionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConditionRepository extends JpaRepository<ConditionEntity, UUID>, JpaSpecificationExecutor<ConditionEntity> {
    List<ConditionEntity> findByPatientId(UUID patientId);

    @Query(value = "SELECT TOP 1 c.recorded_date FROM [condition] c WHERE c.patient_id = :patientId " +
            "AND c.recorded_date IS NOT NULL ORDER BY c.recorded_date DESC", nativeQuery = true)
    Optional<LocalDate> findLatestRecordedDateByPatientId(UUID patientId);
}

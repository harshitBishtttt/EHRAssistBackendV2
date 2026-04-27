package ehrAssist.repository;

import ehrAssist.entity.CareGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CareGoalRepository extends JpaRepository<CareGoalEntity, UUID> {

    List<CareGoalEntity> findByPatientIdAndStatusNotInOrderByCreatedAtAsc(UUID patientId, List<String> excludedStatuses);
}

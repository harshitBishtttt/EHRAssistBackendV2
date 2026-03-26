package ehrAssist.repository;

import ehrAssist.entity.ConditionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConditionRepository extends JpaRepository<ConditionEntity, UUID>, JpaSpecificationExecutor<ConditionEntity> {
    List<ConditionEntity> findByPatientId(UUID patientId);
}

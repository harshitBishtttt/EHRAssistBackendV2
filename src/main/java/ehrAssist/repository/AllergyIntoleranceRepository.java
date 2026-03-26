package ehrAssist.repository;

import ehrAssist.entity.AllergyIntoleranceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AllergyIntoleranceRepository extends JpaRepository<AllergyIntoleranceEntity, UUID>, JpaSpecificationExecutor<AllergyIntoleranceEntity> {
    List<AllergyIntoleranceEntity> findByPatientId(UUID patientId);
}

package ehrAssist.repository;

import ehrAssist.entity.ImmunizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImmunizationRepository extends JpaRepository<ImmunizationEntity, UUID>, JpaSpecificationExecutor<ImmunizationEntity> {
    List<ImmunizationEntity> findByPatientId(UUID patientId);
}

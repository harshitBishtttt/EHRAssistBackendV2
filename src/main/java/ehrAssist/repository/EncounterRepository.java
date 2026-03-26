package ehrAssist.repository;

import ehrAssist.entity.EncounterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EncounterRepository extends JpaRepository<EncounterEntity, UUID>, JpaSpecificationExecutor<EncounterEntity> {
    List<EncounterEntity> findByPatientId(UUID patientId);
}

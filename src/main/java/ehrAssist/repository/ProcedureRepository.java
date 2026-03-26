package ehrAssist.repository;

import ehrAssist.entity.ProcedureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProcedureRepository extends JpaRepository<ProcedureEntity, UUID>, JpaSpecificationExecutor<ProcedureEntity> {
    List<ProcedureEntity> findByPatientId(UUID patientId);
}

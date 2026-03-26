package ehrAssist.repository;

import ehrAssist.entity.MedicationRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationRequestRepository extends JpaRepository<MedicationRequestEntity, UUID>, JpaSpecificationExecutor<MedicationRequestEntity> {
    List<MedicationRequestEntity> findByPatientId(UUID patientId);
}

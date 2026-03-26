package ehrAssist.repository;

import ehrAssist.entity.ObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ObservationRepository extends JpaRepository<ObservationEntity, UUID>, JpaSpecificationExecutor<ObservationEntity> {
    List<ObservationEntity> findByPatientId(UUID patientId);
}

package ehrAssist.repository;

import ehrAssist.entity.ServiceRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequestEntity, UUID>, JpaSpecificationExecutor<ServiceRequestEntity> {
    List<ServiceRequestEntity> findByPatientId(UUID patientId);
    List<ServiceRequestEntity> findByPatientIdIn(List<UUID> patientIds);
}

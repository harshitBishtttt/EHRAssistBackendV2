package ehrAssist.repository;

import ehrAssist.entity.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<AppointmentEntity, UUID>, JpaSpecificationExecutor<AppointmentEntity> {
    List<AppointmentEntity> findByPatientId(UUID patientId);
    List<AppointmentEntity> findByPatientIdIn(List<UUID> patientIds);
}

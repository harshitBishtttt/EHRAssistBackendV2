package ehrAssist.repository;

import ehrAssist.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, UUID>, JpaSpecificationExecutor<PatientEntity> {
    List<PatientEntity> findByPrimaryPractitionerId(UUID practitionerId);
    List<PatientEntity> findByManagingOrganizationId(UUID organizationId);
}

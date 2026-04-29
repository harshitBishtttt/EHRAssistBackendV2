package ehrAssist.repository;

import ehrAssist.entity.PatientEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, UUID>, JpaSpecificationExecutor<PatientEntity> {
    List<PatientEntity> findByPrimaryPractitionerId(UUID practitionerId);
    List<PatientEntity> findByManagingOrganizationId(UUID organizationId);

    @EntityGraph(attributePaths = "names")
    Optional<PatientEntity> findWithNamesById(UUID id);

    @EntityGraph(attributePaths = "names")
    List<PatientEntity> findWithNamesByPrimaryPractitionerId(UUID practitionerId);

    @EntityGraph(attributePaths = "names")
    List<PatientEntity> findWithNamesByManagingOrganizationId(UUID organizationId);

    @Query("SELECT p.id FROM PatientEntity p " +
           "WHERE p.primaryPractitioner.id = :practitionerId " +
           "AND p.active = true AND p.deceasedFlag = false")
    List<UUID> findActivePatientIdsByPractitionerId(@Param("practitionerId") UUID practitionerId);
}

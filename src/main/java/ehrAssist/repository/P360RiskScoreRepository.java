package ehrAssist.repository;

import ehrAssist.entity.P360RiskScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface P360RiskScoreRepository extends JpaRepository<P360RiskScoreEntity, UUID>,
        JpaSpecificationExecutor<P360RiskScoreEntity> {

    @Query(value = "SELECT TOP 1 * FROM p360_risk_score " +
            "WHERE patient_id = :patientId " +
            "AND care_manager_id = :careManagerId " +
            "AND organization_id = :orgId " +
            "ORDER BY created_date DESC",
            nativeQuery = true)
    Optional<P360RiskScoreEntity> findLatestByCareManager(
            @Param("patientId") UUID patientId,
            @Param("careManagerId") UUID careManagerId,
            @Param("orgId") UUID orgId);

    @Query(value = "SELECT TOP 1 * FROM p360_risk_score " +
            "WHERE patient_id = :patientId " +
            "AND practitioner_id = :practitionerId " +
            "AND organization_id = :orgId " +
            "ORDER BY created_date DESC",
            nativeQuery = true)
    Optional<P360RiskScoreEntity> findLatestByPractitioner(
            @Param("patientId") UUID patientId,
            @Param("practitionerId") UUID practitionerId,
            @Param("orgId") UUID orgId);
}

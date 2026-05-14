package ehrAssist.repository;

import ehrAssist.entity.EncounterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EncounterRepository extends JpaRepository<EncounterEntity, UUID>, JpaSpecificationExecutor<EncounterEntity> {
    List<EncounterEntity> findByPatientId(UUID patientId);

    @Query(value = """
            SELECT COUNT(e.id)
            FROM patient AS p
            JOIN encounter AS e ON e.patient_id = p.id
            WHERE p.managing_organization_id = :organizationId
              AND e.status = :status
              AND e.period_start >= :startDate
              AND e.period_start <= :endDate
            """, nativeQuery = true)
    Long countByOrganizationAndStatusAndDateRange(
            @Param("organizationId") UUID organizationId,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = """
            SELECT COUNT(e.id)
            FROM patient AS p
            JOIN encounter AS e ON e.patient_id = p.id
            WHERE p.managing_organization_id = :organizationId
              AND e.period_start >= :startDate
              AND e.period_start <= :endDate
            """, nativeQuery = true)
    Long countByOrganizationAndDateRange(
            @Param("organizationId") UUID organizationId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}

package ehrAssist.repository;

import ehrAssist.dto.projection.RiskFeedProjection;
import ehrAssist.entity.ObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObservationRepository extends JpaRepository<ObservationEntity, UUID>, JpaSpecificationExecutor<ObservationEntity> {
    List<ObservationEntity> findByPatientId(UUID patientId);

    @Query("select o from ObservationEntity o join fetch o.codeMaster where o.patient.id in :patientIds")
    List<ObservationEntity> findAllByPatientIdInWithCodeMaster(List<UUID> patientIds);

    @Query(value = "SELECT TOP 1 o.issued FROM observation o WHERE o.patient_id = :patientId " +
            "AND o.issued IS NOT NULL ORDER BY o.issued DESC", nativeQuery = true)
    Optional<LocalDateTime> findLatestIssuedDateByPatientId(UUID patientId);

    @Query(value = """
            WITH patient_info AS (
                SELECT
                    p.id                                                            AS patient_id,
                    p.gender,
                    TRIM(ISNULL(pn.given_first, '') + ' ' + ISNULL(pn.family, '')) AS full_name,
                    MAX(CASE WHEN pt.system = 'phone' THEN pt.value END)            AS phone,
                    MAX(CASE WHEN pt.system = 'email' THEN pt.value END)            AS email
                FROM dbo.patient p
                LEFT JOIN dbo.patient_name pn
                    ON  pn.patient_id = p.id
                    AND pn.use_type   = 'official'
                LEFT JOIN dbo.patient_telecom pt
                    ON  pt.patient_id = p.id
                WHERE p.id IN (:patientIds)
                GROUP BY p.id, p.gender, pn.family, pn.given_first
            )
            SELECT
                CAST(pi.patient_id AS VARCHAR(36)) AS patientId,
                pi.full_name                       AS fullName,
                pi.gender                          AS gender,
                pi.phone                           AS phone,
                pi.email                           AS email,
                ocm.loinc_code                     AS loincCode,
                ocm.code_system                    AS codeSystem,
                ocm.code_display                   AS display,
                obs.value_quantity                 AS valueQuantity,
                obs.value_unit                     AS unit,
                obs.effective_date                 AS effectiveDate,
                ocm.reference_range_low            AS referenceRangeLow,
                ocm.reference_range_high           AS referenceRangeHigh
            FROM dbo.observation obs
            INNER JOIN dbo.observation_code_master ocm ON ocm.id  = obs.observation_code_id
            INNER JOIN patient_info pi                 ON pi.patient_id = obs.patient_id
            WHERE obs.patient_id  IN (:patientIds)
              AND obs.effective_date >= :windowStart
              AND obs.effective_date <= :windowEnd
            ORDER BY pi.patient_id, ocm.loinc_code, obs.effective_date DESC
            """, nativeQuery = true)
    List<RiskFeedProjection> findRiskFeedByPatientIds(
            @Param("patientIds")  List<UUID>      patientIds,
            @Param("windowStart") LocalDateTime   windowStart,
            @Param("windowEnd")   LocalDateTime   windowEnd);
}

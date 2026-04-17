package ehrAssist.repository;

import ehrAssist.entity.VitalsEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VitalsRepository extends JpaRepository<VitalsEntity, UUID> {

    @Query(value = "SELECT v.* " +
            "FROM vitals AS v " +
            "INNER JOIN observation_code_master AS ocm ON v.observation_code_id = ocm.item_id " +
            "WHERE (:patientId IS NULL OR v.patient_id = :patientId) " +
            "AND (:loincCode IS NULL OR :loincCode = '' OR ocm.loinc_code = :loincCode)",
            countQuery = "SELECT COUNT(1) " +
                    "FROM vitals AS v " +
                    "INNER JOIN observation_code_master AS ocm ON v.observation_code_id = ocm.item_id " +
                    "WHERE (:patientId IS NULL OR v.patient_id = :patientId) " +
                    "AND (:loincCode IS NULL OR :loincCode = '' OR ocm.loinc_code = :loincCode)",
            nativeQuery = true)
    Page<VitalsEntity> searchVitals(UUID patientId, String loincCode, Pageable pageable);
}

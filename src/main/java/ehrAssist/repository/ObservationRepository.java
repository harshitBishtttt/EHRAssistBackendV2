package ehrAssist.repository;

import ehrAssist.entity.ObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObservationRepository extends JpaRepository<ObservationEntity, UUID>, JpaSpecificationExecutor<ObservationEntity> {
    List<ObservationEntity> findByPatientId(UUID patientId);

    @Query(value = "SELECT TOP 1 o.issued FROM observation o WHERE o.patient_id = :patientId " +
            "AND o.issued IS NOT NULL ORDER BY o.issued DESC", nativeQuery = true)
    Optional<LocalDateTime> findLatestIssuedDateByPatientId(UUID patientId);
}

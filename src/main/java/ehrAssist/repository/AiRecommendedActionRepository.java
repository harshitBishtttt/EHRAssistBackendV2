package ehrAssist.repository;

import ehrAssist.entity.AiRecommendedActionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiRecommendedActionRepository extends JpaRepository<AiRecommendedActionEntity, UUID> {

    Page<AiRecommendedActionEntity> findByPatientId(UUID patientId, Pageable pageable);
}

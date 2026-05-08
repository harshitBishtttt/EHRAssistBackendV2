package ehrAssist.repository;

import ehrAssist.entity.AiRecommendationInstructionsEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiRecommendationInstructionsRepository extends JpaRepository<AiRecommendationInstructionsEntity, UUID> {

    Page<AiRecommendationInstructionsEntity> findByPatientId(UUID patientId, Pageable pageable);
}

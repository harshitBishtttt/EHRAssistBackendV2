package ehrAssist.repository;

import ehrAssist.entity.AiRecommendationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiRecommendationRepository extends JpaRepository<AiRecommendationEntity, UUID> {

    Page<AiRecommendationEntity> findByPatientId(UUID patientId, Pageable pageable);
}

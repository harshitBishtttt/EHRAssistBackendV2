package ehrAssist.repository;

import ehrAssist.entity.AiRecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiRecommendationRepository extends JpaRepository<AiRecommendationEntity, UUID> {

    List<AiRecommendationEntity> findByPatientId(UUID patientId);

    List<AiRecommendationEntity> findByPatientIdAndVerifiedAtIsNotNull(UUID patientId);
}

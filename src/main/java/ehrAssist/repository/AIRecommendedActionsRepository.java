package ehrAssist.repository;

import ehrAssist.entity.AIRecommendedActionsEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AIRecommendedActionsRepository extends JpaRepository<AIRecommendedActionsEntity, UUID> {
    Optional<List<AIRecommendedActionsEntity>> findByPatientIdAndStatus(UUID patientId, String status);

    @Modifying   //Tells Spring this is UPDATE/DELETE operation on a select so it can accept void
    @Transactional
    @Query(value = "UPDATE patient_360_ai_recommended_actions SET status = :status WHERE id = :actionId "
            , nativeQuery = true)
    void updateTheTaskStaus(UUID actionId, String status);
}

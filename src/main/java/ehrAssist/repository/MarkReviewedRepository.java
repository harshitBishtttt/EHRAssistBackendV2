package ehrAssist.repository;

import ehrAssist.entity.MarkReviewedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface MarkReviewedRepository extends JpaRepository<MarkReviewedEntity, UUID> {
    @Query(value = "SELECT top 1 mr.* FROM mark_reviewed as mr where patient_id = :patientId " + " order BY created_date desc ; ", nativeQuery = true)
    Optional<MarkReviewedEntity> getTheLatestReview(UUID patientId);
}

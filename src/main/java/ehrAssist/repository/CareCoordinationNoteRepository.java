package ehrAssist.repository;

import ehrAssist.entity.CareCoordinationNoteEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CareCoordinationNoteRepository extends JpaRepository<CareCoordinationNoteEntity, UUID> {
    List<CareCoordinationNoteEntity> findByPatientId(UUID patientId);

    List<CareCoordinationNoteEntity> findByIsActiveAndPatientIdAndCoordinatorEmailAndAiRecommendedActionsEntity_IdAndStatus
            (Boolean isActive, UUID patientId, String coordinatorEmail, UUID actionId, String status);

    @Modifying
    @Transactional
    @Query(value = "UPDATE care_coordination_notes SET is_active = false " +
            "WHERE coordinator_email =:email " +
            "and  patient_id = :patientId  " +
            "and recommendation_note_id = :actionId " +
            "and status = :status "
            , nativeQuery = true)
    void deactivateTheActivity(String email, UUID patientId, UUID actionId, String status);
}

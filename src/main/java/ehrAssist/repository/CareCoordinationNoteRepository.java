package ehrAssist.repository;

import ehrAssist.entity.CareCoordinationNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CareCoordinationNoteRepository extends JpaRepository<CareCoordinationNoteEntity, UUID> {
    List<CareCoordinationNoteEntity> findByPatientId(UUID patientId);
    List<CareCoordinationNoteEntity> findByPatientIdAndCoordinatorEmail(UUID patientId, String coordinatorEmail);
}

package ehrAssist.repository;

import ehrAssist.entity.DocumentReferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentReferenceRepository extends JpaRepository<DocumentReferenceEntity, UUID>, JpaSpecificationExecutor<DocumentReferenceEntity> {
    List<DocumentReferenceEntity> findByPatientId(UUID patientId);
}

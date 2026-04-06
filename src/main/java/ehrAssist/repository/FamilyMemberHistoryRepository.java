package ehrAssist.repository;

import ehrAssist.entity.FamilyMemberHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FamilyMemberHistoryRepository extends JpaRepository<FamilyMemberHistoryEntity, UUID>, JpaSpecificationExecutor<FamilyMemberHistoryEntity> {
    List<FamilyMemberHistoryEntity> findByPatientId(UUID patientId);
}

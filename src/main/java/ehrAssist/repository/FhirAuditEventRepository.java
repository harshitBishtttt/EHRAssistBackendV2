package ehrAssist.repository;

import ehrAssist.entity.FhirAuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FhirAuditEventRepository extends JpaRepository<FhirAuditEventEntity, UUID> {
}

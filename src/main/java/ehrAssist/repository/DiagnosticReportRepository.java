package ehrAssist.repository;

import ehrAssist.entity.DiagnosticReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiagnosticReportRepository extends JpaRepository<DiagnosticReportEntity, UUID>, JpaSpecificationExecutor<DiagnosticReportEntity> {
    List<DiagnosticReportEntity> findByPatientId(UUID patientId);
}

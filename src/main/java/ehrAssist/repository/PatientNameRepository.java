package ehrAssist.repository;

import ehrAssist.entity.PatientNameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PatientNameRepository extends JpaRepository<PatientNameEntity, UUID> {
}

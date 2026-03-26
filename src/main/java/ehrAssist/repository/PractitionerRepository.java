package ehrAssist.repository;

import ehrAssist.entity.PractitionerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PractitionerRepository extends JpaRepository<PractitionerEntity, UUID>, JpaSpecificationExecutor<PractitionerEntity> {
}

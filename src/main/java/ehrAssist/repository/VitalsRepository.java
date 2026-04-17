package ehrAssist.repository;

import ehrAssist.entity.VitalsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VitalsRepository extends JpaRepository<VitalsEntity, UUID>, JpaSpecificationExecutor<VitalsEntity> {
}

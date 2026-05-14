package ehrAssist.repository;

import ehrAssist.entity.P360RiskScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface P360RiskScoreRepository extends JpaRepository<P360RiskScoreEntity, UUID>,
        JpaSpecificationExecutor<P360RiskScoreEntity> {
}

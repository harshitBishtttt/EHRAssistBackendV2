package ehrAssist.repository.master;

import ehrAssist.entity.master.ConditionCodeMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConditionCodeMasterRepository extends JpaRepository<ConditionCodeMasterEntity, Integer> {
    Optional<ConditionCodeMasterEntity> findByCodeSystemAndCodeValue(String codeSystem, String codeValue);
}

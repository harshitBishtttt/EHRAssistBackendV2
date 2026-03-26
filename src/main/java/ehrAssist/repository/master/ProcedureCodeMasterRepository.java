package ehrAssist.repository.master;

import ehrAssist.entity.master.ProcedureCodeMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcedureCodeMasterRepository extends JpaRepository<ProcedureCodeMasterEntity, Integer> {
    Optional<ProcedureCodeMasterEntity> findFirstByMinCodeLessThanEqualAndMaxCodeGreaterThanEqual(Integer code1, Integer code2);
}

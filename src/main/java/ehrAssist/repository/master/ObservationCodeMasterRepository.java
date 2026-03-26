package ehrAssist.repository.master;

import ehrAssist.entity.master.ObservationCodeMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ObservationCodeMasterRepository extends JpaRepository<ObservationCodeMasterEntity, Integer> {
    Optional<ObservationCodeMasterEntity> findByCodeSystemAndLoincCode(String codeSystem, String loincCode);
    Optional<ObservationCodeMasterEntity> findByItemId(Integer itemId);
}

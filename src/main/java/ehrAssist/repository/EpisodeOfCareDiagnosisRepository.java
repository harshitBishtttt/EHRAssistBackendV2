package ehrAssist.repository;

import ehrAssist.entity.EpisodeOfCareDiagnosisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EpisodeOfCareDiagnosisRepository extends JpaRepository<EpisodeOfCareDiagnosisEntity, UUID> {

    @Query("SELECT d FROM EpisodeOfCareDiagnosisEntity d JOIN FETCH d.condition WHERE d.episode.id = :episodeId ORDER BY d.rank ASC, d.id ASC")
    List<EpisodeOfCareDiagnosisEntity> findByEpisodeIdWithCondition(@Param("episodeId") UUID episodeId);
}

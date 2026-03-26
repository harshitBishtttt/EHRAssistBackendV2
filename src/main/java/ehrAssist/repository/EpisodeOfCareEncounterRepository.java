package ehrAssist.repository;

import ehrAssist.entity.EpisodeOfCareEncounterEntity;
import ehrAssist.entity.EpisodeOfCareEncounterPk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EpisodeOfCareEncounterRepository extends JpaRepository<EpisodeOfCareEncounterEntity, EpisodeOfCareEncounterPk> {

    List<EpisodeOfCareEncounterEntity> findByEpisodeIdOrderByEncounterIdAsc(UUID episodeId);
}

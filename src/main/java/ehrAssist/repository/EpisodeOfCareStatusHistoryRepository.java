package ehrAssist.repository;

import ehrAssist.entity.EpisodeOfCareStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EpisodeOfCareStatusHistoryRepository extends JpaRepository<EpisodeOfCareStatusHistoryEntity, UUID> {

    List<EpisodeOfCareStatusHistoryEntity> findByEpisodeIdOrderByPeriodStartAsc(UUID episodeId);

    List<EpisodeOfCareStatusHistoryEntity> findByEpisodeIdInOrderByPeriodStartAsc(List<UUID> episodeIds);
}

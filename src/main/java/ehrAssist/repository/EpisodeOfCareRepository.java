package ehrAssist.repository;

import ehrAssist.entity.EpisodeOfCareEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EpisodeOfCareRepository extends JpaRepository<EpisodeOfCareEntity, UUID>, JpaSpecificationExecutor<EpisodeOfCareEntity> {

    @Query("SELECT DISTINCT e FROM EpisodeOfCareEntity e "
            + "LEFT JOIN FETCH e.patient "
            + "LEFT JOIN FETCH e.managingOrganization "
            + "LEFT JOIN FETCH e.careManager "
            + "WHERE e.id = :id")
    Optional<EpisodeOfCareEntity> findFetchedById(@Param("id") UUID id);

    @Query("SELECT DISTINCT e FROM EpisodeOfCareEntity e "
            + "LEFT JOIN FETCH e.patient "
            + "LEFT JOIN FETCH e.managingOrganization "
            + "LEFT JOIN FETCH e.careManager "
            + "WHERE e.id IN :ids")
    List<EpisodeOfCareEntity> findAllWithAssociationsByIds(@Param("ids") List<UUID> ids);
}

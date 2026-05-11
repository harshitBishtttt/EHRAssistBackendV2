package ehrAssist.repository;

import ehrAssist.dto.projection.CareManagerOrganizationProjection;
import ehrAssist.entity.OrganizationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {

    @Query(value = """
            SELECT cm.care_manager_id,
                   o.id AS id,
                   o.active AS active,
                   o.name AS name,
                   o.type_code AS typeCode,
                   o.type_display AS typeDisplay,
                   o.phone AS phone,
                   o.address_city AS addressCity,
                   o.address_state AS addressState,
                   o.version AS version,
                   o.created_at AS createdAt
            FROM dbo.organization o
            INNER JOIN dbo.care_manager_organization_mapper cm
                ON cm.organization_id = o.id
            WHERE cm.care_manager_id = :careManagerId
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM dbo.organization o
            INNER JOIN dbo.care_manager_organization_mapper cm
                ON cm.organization_id = o.id
            WHERE cm.care_manager_id = :careManagerId
            """,
            nativeQuery = true)
    Page<CareManagerOrganizationProjection> findOrganizationsByCareManagerId(
            UUID careManagerId, Pageable pageable);
}


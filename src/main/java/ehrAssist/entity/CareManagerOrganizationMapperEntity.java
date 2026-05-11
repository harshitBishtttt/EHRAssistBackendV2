package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "care_manager_organization_mapper")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareManagerOrganizationMapperEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "care_manager_id", nullable = false)
    private UUID careManagerId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
}

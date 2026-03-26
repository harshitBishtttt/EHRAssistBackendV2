package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "episode_of_care")
public class EpisodeOfCareEntity extends BaseEntity {

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managing_organization_id")
    private OrganizationEntity managingOrganization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_manager_id")
    private PractitionerEntity careManager;

    @Column(name = "type_system")
    private String typeSystem;

    @Column(name = "type_code", length = 50)
    private String typeCode;

    @Column(name = "type_display", length = 200)
    private String typeDisplay;

    @Column(name = "type_text", columnDefinition = "NVARCHAR(500)")
    private String typeText;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;
}

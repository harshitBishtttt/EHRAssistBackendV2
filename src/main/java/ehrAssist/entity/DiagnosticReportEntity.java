package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "diagnostic_report")
public class DiagnosticReportEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id")
    private EncounterEntity encounter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performer_id")
    private PractitionerEntity performer;

    @Column(name = "status")
    private String status;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "code_system")
    private String codeSystem;

    @Column(name = "code_value")
    private String codeValue;

    @Column(name = "code_display")
    private String codeDisplay;

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    @Column(name = "issued")
    private LocalDateTime issued;

    @Column(name = "conclusion", columnDefinition = "NVARCHAR(MAX)")
    private String conclusion;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "diagnostic_report_observation",
            joinColumns = @JoinColumn(name = "report_id"),
            inverseJoinColumns = @JoinColumn(name = "observation_id")
    )
    private List<ObservationEntity> observations = new ArrayList<>();
}

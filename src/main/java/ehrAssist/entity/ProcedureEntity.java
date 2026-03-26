package ehrAssist.entity;

import ehrAssist.entity.master.ProcedureCodeMasterEntity;
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
@Table(name = "[procedure]")
public class ProcedureEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id")
    private EncounterEntity encounter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performer_id")
    private PractitionerEntity performer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "procedure_code_id")
    private ProcedureCodeMasterEntity codeMaster;

    @Column(name = "cpt_code")
    private Integer cptCode;

    @Column(name = "status")
    private String status;

    @Column(name = "description")
    private String description;

    @Column(name = "body_site_code")
    private String bodySiteCode;

    @Column(name = "body_site_display")
    private String bodySiteDisplay;

    @Column(name = "outcome_code")
    private String outcomeCode;

    @Column(name = "performed_start")
    private LocalDateTime performedStart;

    @Column(name = "performed_end")
    private LocalDateTime performedEnd;
}

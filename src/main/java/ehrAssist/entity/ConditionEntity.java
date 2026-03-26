package ehrAssist.entity;

import ehrAssist.entity.master.ConditionCodeMasterEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "[condition]")
public class ConditionEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id")
    private EncounterEntity encounter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorder_id")
    private PractitionerEntity recorder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "condition_code_id")
    private ConditionCodeMasterEntity codeMaster;

    @Column(name = "clinical_status")
    private String clinicalStatus;

    @Column(name = "verification_status")
    private String verificationStatus;

    @Column(name = "severity_code")
    private String severityCode;

    @Column(name = "severity_display")
    private String severityDisplay;

    @Column(name = "seq_num")
    private Integer seqNum;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "abatement_date")
    private LocalDate abatementDate;

    @Column(name = "recorded_date")
    private LocalDate recordedDate;
}

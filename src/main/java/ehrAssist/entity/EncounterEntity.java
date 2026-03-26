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
@Table(name = "encounter")
public class EncounterEntity extends BaseEntity {

    @Column(name = "status")
    private String status;

    @Column(name = "encounter_class")
    private String encounterClass;

    @Column(name = "type_code")
    private String typeCode;

    @Column(name = "type_display")
    private String typeDisplay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id")
    private PractitionerEntity practitioner;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @Column(name = "admission_location")
    private String admissionLocation;

    @Column(name = "discharge_location")
    private String dischargeLocation;

    @Column(name = "discharge_disposition_code")
    private String dischargeDispositionCode;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "reason_display")
    private String reasonDisplay;

    @Column(name = "diagnosis_text")
    private String diagnosisText;

    @Column(name = "insurance")
    private String insurance;

    @Column(name = "clinical_notes", columnDefinition = "NVARCHAR(MAX)")
    private String clinicalNotes;
}

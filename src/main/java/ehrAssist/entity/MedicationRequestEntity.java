package ehrAssist.entity;

import ehrAssist.entity.master.MedicationCodeMasterEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "medication_request")
public class MedicationRequestEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id")
    private EncounterEntity encounter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private PractitionerEntity requester;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "medication_code_id")
    private MedicationCodeMasterEntity medicationCode;

    @Column(name = "status")
    private String status;

    @Column(name = "intent")
    private String intent;

    @Column(name = "priority")
    private String priority;

    @Column(name = "dosage_text")
    private String dosageText;

    @Column(name = "dosage_route_code")
    private String dosageRouteCode;

    @Column(name = "dosage_route_display")
    private String dosageRouteDisplay;

    @Column(name = "dose_unit")
    private String doseUnit;

    @Column(name = "frequency_text")
    private String frequencyText;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "reason_display")
    private String reasonDisplay;

    @Column(name = "dose_value")
    private BigDecimal doseValue;

    @Column(name = "note", columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @Column(name = "authored_on")
    private LocalDateTime authoredOn;

    @Column(name = "valid_start")
    private LocalDateTime validStart;

    @Column(name = "valid_end")
    private LocalDateTime validEnd;
}

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
@Table(name = "service_request")
public class ServiceRequestEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id")
    private EncounterEntity encounter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private PractitionerEntity requester;

    @Column(name = "status")
    private String status;

    @Column(name = "intent")
    private String intent;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "code_system")
    private String codeSystem;

    @Column(name = "code_value")
    private String codeValue;

    @Column(name = "code_display")
    private String codeDisplay;

    @Column(name = "authored_on")
    private LocalDateTime authoredOn;

    @Column(name = "note", columnDefinition = "NVARCHAR(MAX)")
    private String note;
}

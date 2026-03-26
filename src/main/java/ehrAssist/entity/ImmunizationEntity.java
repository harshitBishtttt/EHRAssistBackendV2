package ehrAssist.entity;

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
@Table(name = "immunization")
public class ImmunizationEntity extends BaseEntity {

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

    @Column(name = "vaccine_code_system")
    private String vaccineCodeSystem;

    @Column(name = "vaccine_code_value")
    private String vaccineCodeValue;

    @Column(name = "vaccine_code_display")
    private String vaccineCodeDisplay;

    @Column(name = "lot_number")
    private String lotNumber;

    @Column(name = "site_code")
    private String siteCode;

    @Column(name = "route_code")
    private String routeCode;

    @Column(name = "occurrence_date")
    private LocalDateTime occurrenceDate;

    @Column(name = "dose_quantity")
    private BigDecimal doseQuantity;
}

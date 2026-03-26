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
@Table(name = "appointment")
public class AppointmentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private PatientEntity patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id")
    private PractitionerEntity practitioner;

    @Column(name = "status")
    private String status;

    @Column(name = "service_type_code")
    private String serviceTypeCode;

    @Column(name = "service_type_display")
    private String serviceTypeDisplay;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "reason_display")
    private String reasonDisplay;

    @Column(name = "description")
    private String description;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;
}

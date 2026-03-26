package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "patient")
public class PatientEntity extends BaseEntity {

    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "gender", nullable = false, length = 10)
    private String gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Builder.Default
    @Column(name = "deceased_flag")
    private Boolean deceasedFlag = false;

    @Column(name = "deceased_date")
    private LocalDateTime deceasedDate;

    @Column(name = "marital_status_code", length = 5)
    private String maritalStatusCode;

    @Column(name = "marital_status_display", length = 50)
    private String maritalStatusDisplay;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "language_display", length = 50)
    private String languageDisplay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_practitioner_id")
    private PractitionerEntity primaryPractitioner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managing_organization_id")
    private OrganizationEntity managingOrganization;

    @Builder.Default
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientNameEntity> names = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientAddressEntity> addresses = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientTelecomEntity> telecoms = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PatientIdentifierEntity> identifiers = new ArrayList<>();
}

package ehrAssist.entity;

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
@Table(name = "practitioner")
public class PractitionerEntity extends BaseEntity {

    @Column(name = "family_name")
    private String familyName;

    @Column(name = "given_name")
    private String givenName;

    @Column(name = "gender")
    private String gender;

    @Column(name = "npi", unique = true, nullable = true)
    private String npi;

    @Column(name = "specialty_code")
    private String specialtyCode;

    @Column(name = "specialty_display")
    private String specialtyDisplay;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private OrganizationEntity organization;

    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;
}

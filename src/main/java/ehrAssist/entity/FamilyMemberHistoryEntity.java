package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "family_member_history")
public class FamilyMemberHistoryEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "relationship_system", length = 200)
    private String relationshipSystem;

    @Column(name = "relationship_code", nullable = false, length = 20)
    private String relationshipCode;

    @Column(name = "relationship_display", length = 100)
    private String relationshipDisplay;

    @Column(name = "sex", length = 10)
    private String sex;

    @Column(name = "born_date")
    private LocalDate bornDate;

    @Column(name = "age_value", precision = 5, scale = 1)
    private BigDecimal ageValue;

    @Column(name = "age_unit", length = 10)
    private String ageUnit;

    @Column(name = "estimated_age")
    private Boolean estimatedAge;

    @Column(name = "deceased_flag")
    private Boolean deceasedFlag;

    @Column(name = "deceased_age_value", precision = 5, scale = 1)
    private BigDecimal deceasedAgeValue;

    @Column(name = "deceased_age_unit", length = 10)
    private String deceasedAgeUnit;

    @Column(name = "deceased_date")
    private LocalDate deceasedDate;

    @Column(name = "note", columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @Builder.Default
    @OneToMany(mappedBy = "familyMemberHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FamilyMemberHistoryConditionEntity> conditions = new ArrayList<>();
}

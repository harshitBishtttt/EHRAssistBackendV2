package ehrAssist.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "organization")
public class OrganizationEntity extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "type_code")
    private String typeCode;

    @Column(name = "type_display")
    private String typeDisplay;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address_city")
    private String addressCity;

    @Column(name = "address_state")
    private String addressState;

    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;
}

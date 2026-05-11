package ehrAssist.mapper;

import ehrAssist.dto.projection.CareManagerOrganizationProjection;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.time.ZoneId;
import java.util.Date;

@Component
public class OrganizationMapper {

    public Organization projectionToFhirResource(CareManagerOrganizationProjection projection) {
        Organization org = new Organization();

        org.setId(projection.getId().toString());

        Meta meta = new Meta();
        if (!ObjectUtils.isEmpty(projection.getVersion())) {
            meta.setVersionId(projection.getVersion().toString());
        }
        if (!ObjectUtils.isEmpty(projection.getCreatedAt())) {
            meta.setLastUpdated(Date.from(projection.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()));
        }
        org.setMeta(meta);

        if (!ObjectUtils.isEmpty(projection.getActive())) {
            org.setActive(projection.getActive());
        }

        if (!ObjectUtils.isEmpty(projection.getName())) {
            org.setName(projection.getName());
        }

        if (!ObjectUtils.isEmpty(projection.getTypeCode())) {
            org.addType(new CodeableConcept().addCoding(
                    new Coding()
                            .setCode(projection.getTypeCode())
                            .setDisplay(projection.getTypeDisplay())
            ));
        }

        if (!ObjectUtils.isEmpty(projection.getPhone())) {
            org.addTelecom(new ContactPoint()
                    .setSystem(ContactPoint.ContactPointSystem.PHONE)
                    .setValue(projection.getPhone()));
        }

        if (!ObjectUtils.isEmpty(projection.getAddressCity()) || !ObjectUtils.isEmpty(projection.getAddressState())) {
            Address address = org.addAddress();
            if (!ObjectUtils.isEmpty(projection.getAddressCity())) {
                address.setCity(projection.getAddressCity());
            }
            if (!ObjectUtils.isEmpty(projection.getAddressState())) {
                address.setState(projection.getAddressState());
            }
        }

        return org;
    }
}

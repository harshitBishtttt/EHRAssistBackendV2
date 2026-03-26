package ehrAssist.service.impl;

import ehrAssist.entity.PractitionerEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.PractitionerMapper;
import ehrAssist.repository.OrganizationRepository;
import ehrAssist.repository.PractitionerRepository;
import ehrAssist.service.PractitionerService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PractitionerServiceImpl implements PractitionerService {

    private final PractitionerRepository practitionerRepository;
    private final OrganizationRepository organizationRepository;
    private final PractitionerMapper practitionerMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public Practitioner getById(UUID id) {
        PractitionerEntity entity = practitionerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + id));
        return practitionerMapper.toFhirResource(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, String name, String specialty, Pageable pageable) {
        if (id == null && name == null && specialty == null) {
            return bundleBuilder.searchSetWithPagination("Practitioner", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        Specification<PractitionerEntity> spec = Specification.where(null);

        if (id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), id));
        }
        if (name != null) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("familyName")), "%" + name.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("givenName")), "%" + name.toLowerCase() + "%")
            ));
        }
        if (specialty != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("specialtyCode"), specialty));
        }

        Page<PractitionerEntity> pageResult = practitionerRepository.findAll(spec, pageable);

        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(practitionerMapper::toFhirResource)
                .map(Resource.class::cast)
                .toList();

        StringBuilder queryParams = new StringBuilder();
        if (id != null) queryParams.append("_id=").append(id).append("&");
        if (name != null) queryParams.append("name=").append(name).append("&");
        if (specialty != null) queryParams.append("specialty=").append(specialty).append("&");
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination("Practitioner", fhirResources, pageResult.getTotalElements(), 
                pageable.getPageNumber(), pageable.getPageSize(), query);
    }

    @Override
    public Practitioner create(Practitioner resource) {
        PractitionerEntity entity = practitionerMapper.toEntity(resource);
        PractitionerEntity saved = practitionerRepository.save(entity);
        return practitionerMapper.toFhirResource(saved);
    }

    @Override
    public Practitioner update(UUID id, Practitioner resource) {
        PractitionerEntity existing = practitionerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + id));

        PractitionerEntity updated = practitionerMapper.toEntity(resource);

        existing.setFamilyName(updated.getFamilyName());
        existing.setGivenName(updated.getGivenName());
        existing.setGender(updated.getGender());
        existing.setNpi(updated.getNpi());
        existing.setSpecialtyCode(updated.getSpecialtyCode());
        existing.setSpecialtyDisplay(updated.getSpecialtyDisplay());
        existing.setPhone(updated.getPhone());
        existing.setEmail(updated.getEmail());
        existing.setBirthDate(updated.getBirthDate());
        existing.setActive(updated.getActive());

        PractitionerEntity saved = practitionerRepository.save(existing);
        return practitionerMapper.toFhirResource(saved);
    }

    @Override
    public void delete(UUID id) {
        PractitionerEntity entity = practitionerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found: " + id));
        practitionerRepository.delete(entity);
    }
}

package ehrAssist.service.impl;

import ehrAssist.entity.EpisodeOfCareDiagnosisEntity;
import ehrAssist.entity.EpisodeOfCareEntity;
import ehrAssist.entity.EpisodeOfCareStatusHistoryEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.mapper.EpisodeOfCareMapper;
import ehrAssist.repository.EpisodeOfCareDiagnosisRepository;
import ehrAssist.repository.EpisodeOfCareRepository;
import ehrAssist.repository.EpisodeOfCareStatusHistoryRepository;
import ehrAssist.service.EpisodeOfCareService;
import ehrAssist.util.BundleBuilder;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EpisodeOfCareServiceImpl implements EpisodeOfCareService {

    private final EpisodeOfCareRepository episodeOfCareRepository;
    private final EpisodeOfCareDiagnosisRepository episodeOfCareDiagnosisRepository;
    private final EpisodeOfCareStatusHistoryRepository statusHistoryRepository;
    private final EpisodeOfCareMapper episodeOfCareMapper;
    private final BundleBuilder bundleBuilder;

    @Override
    @Transactional(readOnly = true)
    public EpisodeOfCare getById(UUID id) {
        EpisodeOfCareEntity entity = episodeOfCareRepository.findFetchedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EpisodeOfCare not found: " + id));
        List<EpisodeOfCareDiagnosisEntity> diagnoses =
                episodeOfCareDiagnosisRepository.findByEpisodeIdWithCondition(id);
        List<EpisodeOfCareStatusHistoryEntity> statusHistory =
                statusHistoryRepository.findByEpisodeIdOrderByPeriodStartAsc(id);
        return episodeOfCareMapper.toFhirResource(entity, diagnoses, statusHistory);
    }

    @Override
    @Transactional(readOnly = true)
    public Bundle search(UUID id, UUID patientId, String status, String type, Pageable pageable) {
        if (id == null && patientId == null && status == null && type == null) {
            return bundleBuilder.searchSetWithPagination("EpisodeOfCare", List.of(), 0L, 0, pageable.getPageSize(), "");
        }

        Specification<EpisodeOfCareEntity> spec = Specification.where(null);

        if (id != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("id"), id));
        }
        if (patientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("patient").get("id"), patientId));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.trim()));
        }
        if (type != null && !type.isBlank()) {
            String t = type.trim();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("typeCode"), t));
        }

        Page<EpisodeOfCareEntity> pageResult = episodeOfCareRepository.findAll(spec, pageable);

        List<UUID> ids = pageResult.getContent().stream().map(EpisodeOfCareEntity::getId).toList();
        Map<UUID, EpisodeOfCareEntity> byId = ids.isEmpty()
                ? Map.of()
                : episodeOfCareRepository.findAllWithAssociationsByIds(ids).stream()
                .collect(Collectors.toMap(EpisodeOfCareEntity::getId, Function.identity(), (a, b) -> a));

        Map<UUID, List<EpisodeOfCareStatusHistoryEntity>> statusHistoryByEpisode = ids.isEmpty()
                ? Map.of()
                : statusHistoryRepository.findByEpisodeIdInOrderByPeriodStartAsc(ids).stream()
                .collect(Collectors.groupingBy(sh -> sh.getEpisode().getId()));

        List<Resource> fhirResources = pageResult.getContent().stream()
                .map(e -> byId.getOrDefault(e.getId(), e))
                .map(entity -> {
                    List<EpisodeOfCareDiagnosisEntity> diagnoses =
                            episodeOfCareDiagnosisRepository.findByEpisodeIdWithCondition(entity.getId());
                    List<EpisodeOfCareStatusHistoryEntity> statusHistory =
                            statusHistoryByEpisode.getOrDefault(entity.getId(), List.of());
                    return episodeOfCareMapper.toFhirResource(entity, diagnoses, statusHistory);
                })
                .map(Resource.class::cast)
                .toList();

        StringBuilder queryParams = new StringBuilder();
        if (id != null) {
            queryParams.append("_id=").append(id).append("&");
        }
        if (patientId != null) {
            queryParams.append("patient=").append(patientId).append("&");
        }
        if (status != null && !status.isBlank()) {
            queryParams.append("status=").append(status.trim()).append("&");
        }
        if (type != null && !type.isBlank()) {
            queryParams.append("type=").append(type.trim()).append("&");
        }
        String query = queryParams.length() > 0 ? queryParams.substring(0, queryParams.length() - 1) : "";

        return bundleBuilder.searchSetWithPagination(
                "EpisodeOfCare",
                fhirResources,
                pageResult.getTotalElements(),
                pageResult.getNumber(),
                pageResult.getSize(),
                query);
    }
}

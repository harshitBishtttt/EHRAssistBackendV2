package ehrAssist.service.impl;

import ehrAssist.dto.request.AiActionRequest;
import ehrAssist.dto.response.AiActionResponse;
import ehrAssist.entity.AIRecommendedActionsEntity;
import ehrAssist.repository.AIRecommendedActionsRepository;
import ehrAssist.service.AiActionsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AiActionsServiceImpl implements AiActionsService {
    private final AIRecommendedActionsRepository aiRecommendedActionsRepository;

    private AiActionsServiceImpl(AIRecommendedActionsRepository aiRecommendedActionsRepository) {
        this.aiRecommendedActionsRepository = aiRecommendedActionsRepository;
    }

    public List<AiActionResponse> createAiAction(List<AiActionRequest> request) {
        List<AIRecommendedActionsEntity> bulkRecommendation = new ArrayList<>();
        request.forEach(data -> {
            AIRecommendedActionsEntity newRecommendation = new AIRecommendedActionsEntity();
            newRecommendation.setPatientId(data.getPatientId());
            newRecommendation.setPriority(data.getPriority());
            newRecommendation.setStatus("pending");
            newRecommendation.setAction(data.getAction());
            newRecommendation.setDescription(data.getDescription());
            newRecommendation.setAiRationale(data.getAiRationale());
            newRecommendation.setDueDate(data.getDueDate());
            newRecommendation.setCreatedAt(LocalDateTime.now());
            bulkRecommendation.add(newRecommendation);
        });
        List<AIRecommendedActionsEntity> save = aiRecommendedActionsRepository.saveAll(bulkRecommendation);
        return save.stream().map(savedData -> {
            return AiActionResponse.builder()
                    .actionId(savedData.getId())
                    .parentId(savedData.getPatientId())
                    .status(savedData.getStatus())
                    .build();
        }).toList();
    }

    public List<AiActionResponse> getAiActions(UUID patientId, String status) {
        Optional<List<AIRecommendedActionsEntity>> actions = aiRecommendedActionsRepository
                .findByPatientIdAndStatus(patientId, status);
        return actions.map(aiRecommendedActionsEntities ->
                aiRecommendedActionsEntities.stream().map(data -> {
                    return AiActionResponse.builder()
                            .actionId(data.getId())
                            .action(data.getAction())
                            .description(data.getDescription())
                            .aiRationale(data.getAiRationale())
                            .priority(data.getPriority())
                            .status(data.getStatus())
                            .dueDate(data.getDueDate())
                            .parentId(data.getPatientId())
                            .build();
                }).toList()).orElseGet(List::of);
    }

    public void updateTaskStatus(UUID actionId, String status) {
        aiRecommendedActionsRepository.updateTheTaskStaus(actionId, status);
    }

}

package ehrAssist.service;

import ehrAssist.dto.request.AiActionRequest;
import ehrAssist.dto.response.AiActionResponse;

import java.util.List;
import java.util.UUID;

public interface AiActionsService {
    List<AiActionResponse> createAiAction(List<AiActionRequest> request);

    List<AiActionResponse> getAiActions(UUID patientId, String status);

    void updateTaskStatus(UUID actionId, String status);
}

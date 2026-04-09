package ehrAssist.controller;

import ehrAssist.dto.request.AiActionRequest;
import ehrAssist.dto.response.AiActionResponse;
import ehrAssist.service.AiActionsService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/baseR4/portal")
public class PatientThreeSixtyController {

    private final AiActionsService aiActionsService;

    private PatientThreeSixtyController(AiActionsService aiActionsService) {
        this.aiActionsService = aiActionsService;
    }

    @PostMapping(consumes = {"application/fhir+json", "application/json"}, produces = "application/fhir+json", path = "/create-recommendations")
    ResponseEntity<?> createAiAction(@RequestBody List<AiActionRequest> request) {
        List<AiActionResponse> created = aiActionsService.createAiAction(request);
        return ResponseEntity.status(201)
                .header("Content-Type", "application/fhir+json")
                .body(created);
    }


    @GetMapping(path = "/task-queue", produces = "application/fhir+json")
    ResponseEntity<?> getMyTaskQueue(@RequestParam UUID patientId,
                                     @RequestParam String status,
                                     @PageableDefault(page = 0, size = 10) Pageable pageable) {
        List<AiActionResponse> response = aiActionsService.getAiActions(patientId, status);
        return ResponseEntity.status(200).body(response);
    }

    @PatchMapping(path = "/update-task", produces = "application/fhir+json")
    ResponseEntity<?> updateTaskStatus(@RequestParam UUID actionId, @RequestParam String status) {
        aiActionsService.updateTaskStatus(actionId, status);
        return ResponseEntity.status(200).body(null);
    }
}

package ehrAssist.controller;

import ehrAssist.service.LifestyleGoalService;
import ehrAssist.service.MyCarePlanTaskService;
import ehrAssist.util.FhirResponseHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/baseR4/care-plan")
@RequiredArgsConstructor
public class MyCarePlanTaskController {

    private final MyCarePlanTaskService myCarePlanTaskService;
    private final LifestyleGoalService lifestyleGoalService;
    private final FhirResponseHelper fhirResponseHelper;

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER', 'PATIENT')")
    @GetMapping(value = "/tasks", produces = "application/fhir+json")
    public ResponseEntity<String> getTasks(@RequestParam UUID patientId) {
        return fhirResponseHelper.toResponse(myCarePlanTaskService.getCarePlanTasks(patientId));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'CARE_MANAGER', 'PROVIDER', 'PATIENT')")
    @GetMapping(value = "/lifestyle-goals", produces = "application/fhir+json")
    public ResponseEntity<String> getLifestyleGoals(@RequestParam UUID patientId) {
        return fhirResponseHelper.toResponse(lifestyleGoalService.getLifestyleGoals(patientId));
    }
}

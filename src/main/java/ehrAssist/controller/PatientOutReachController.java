package ehrAssist.controller;

import ehrAssist.dto.request.EmailRequest;
import ehrAssist.service.PatientOutReachService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/baseR4/out-reach")
@RequiredArgsConstructor
public class PatientOutReachController {

    private final PatientOutReachService patientOutReachService;

    @PostMapping("/email-portal")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest request) {
        patientOutReachService.sendEmail(request);
        return ResponseEntity.ok("Email sent successfully to " + request.getReceiverEmail());
    }
}

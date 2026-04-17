package ehrAssist.service;

import ehrAssist.dto.request.EmailRequest;

public interface PatientOutReachService {
    void sendEmail(EmailRequest request);
}

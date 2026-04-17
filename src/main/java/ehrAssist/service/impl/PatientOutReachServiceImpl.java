package ehrAssist.service.impl;

import ehrAssist.dto.request.EmailRequest;
import ehrAssist.exception.FhirValidationException;
import ehrAssist.service.PatientOutReachService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientOutReachServiceImpl implements PatientOutReachService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    public void sendEmail(EmailRequest request) {
        if (ObjectUtils.isEmpty(request.getReceiverEmail())) {
            throw new FhirValidationException("receiverEmail is required");
        }
        if (ObjectUtils.isEmpty(request.getSubject())) {
            throw new FhirValidationException("subject is required");
        }
        if (ObjectUtils.isEmpty(request.getBody())) {
            throw new FhirValidationException("body is required");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(request.getReceiverEmail());
            helper.setSubject(request.getSubject());
            helper.setText(request.getBody(), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("MessagingException while sending email: ", e);
            throw new FhirValidationException("Failed to send email: " + e.getMessage());
        } catch (MailException e) {
            log.error("MailException while sending email: ", e);
            throw new FhirValidationException("Failed to send email: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while sending email: ", e);
            throw new FhirValidationException("Failed to send email: " + e.getMessage());
        }
    }
}

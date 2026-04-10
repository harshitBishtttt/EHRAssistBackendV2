package ehrAssist.service;

import ehrAssist.dto.request.MarkReviewedRequest;
import ehrAssist.dto.response.MarkReviewedResponse;

import java.util.UUID;

public interface MarkReviewedService {
    MarkReviewedResponse createReviewed(MarkReviewedRequest req);
    MarkReviewedResponse getPatientReview(UUID patientId);
}

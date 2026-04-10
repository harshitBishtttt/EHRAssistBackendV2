package ehrAssist.service.impl;

import ehrAssist.dto.request.MarkReviewedRequest;
import ehrAssist.dto.response.MarkReviewedResponse;
import ehrAssist.entity.MarkReviewedEntity;
import ehrAssist.repository.MarkReviewedRepository;
import ehrAssist.service.MarkReviewedService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class MarkReviewedServiceImpl implements MarkReviewedService {
    private MarkReviewedRepository markReviewedRepository;

    private MarkReviewedServiceImpl(MarkReviewedRepository markReviewedRepository) {
        this.markReviewedRepository = markReviewedRepository;
    }

    public MarkReviewedResponse createReviewed(MarkReviewedRequest req) {
        MarkReviewedEntity review = new MarkReviewedEntity();
        review.setIsReviewed(true);
        review.setCreatedDate(LocalDateTime.now());
        review.setPatientId(req.getPatientId());
        MarkReviewedEntity save = markReviewedRepository.save(review);
        return MarkReviewedResponse.builder()
                .isReviewed(save.getIsReviewed())
                .parentId(save.getPatientId()).build();
    }

    public MarkReviewedResponse getPatientReview(UUID patientId) {
        Optional<MarkReviewedEntity> latestReview = markReviewedRepository.getTheLatestReview(patientId);
        if (latestReview.isPresent()) {
            MarkReviewedEntity obj = latestReview.get();
            return MarkReviewedResponse.builder()
                    .reviewId(obj.getId())
                    .parentId(obj.getPatientId())
                    .createdDate(obj.getCreatedDate())
                    .isReviewed(obj.getIsReviewed()).build();
        }
        return MarkReviewedResponse.builder()
                .reviewId(null)
                .parentId(null)
                .createdDate(null)
                .isReviewed(false).build();
    }

}

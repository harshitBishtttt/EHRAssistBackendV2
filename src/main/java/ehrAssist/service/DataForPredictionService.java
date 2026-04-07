package ehrAssist.service;

import java.util.Map;
import java.util.UUID;

public interface DataForPredictionService {
    Map<String, Object> buildPredictionData(UUID patientId);
}

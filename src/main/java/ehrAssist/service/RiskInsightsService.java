package ehrAssist.service;

import java.util.UUID;

public interface RiskInsightsService {
    String getCashedRiskInsights(UUID patient_id,String authHeader);
}

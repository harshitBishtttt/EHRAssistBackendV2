package ehrAssist.util;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class SheetTableResolver {

    private static final Map<String, String> SHEET_TO_TABLE_ALIASES = Map.of(
            "care_manager_organization_mappe", "care_manager_organization_mapper"
    );

    private static final Set<String> ALLOWED_ENTITY_TABLES = Set.of(
            "organization",
            "practitioner",
            "patient",
            "patient_identifier",
            "patient_name",
            "patient_address",
            "patient_telecom",
            "encounter",
            "condition",
            "observation",
            "procedure",
            "medication_request",
            "appointment",
            "allergy_intolerance",
            "diagnostic_report",
            "diagnostic_report_observation",
            "service_request",
            "immunization",
            "document_reference",
            "episode_of_care",
            "episode_of_care_diagnosis",
            "episode_of_care_encounter",
            "episode_of_care_status_history",
            "vitals",
            "care_manager_organization_mapper",
            "family_member_history",
            "family_member_history_condition",
            "lifestyle_goal"
    );

    public String resolveTable(String sheetName) {
        if (ObjectUtils.isEmpty(sheetName)) {
            return null;
        }
        String key = normalize(sheetName);
        return SHEET_TO_TABLE_ALIASES.getOrDefault(key, key);
    }

    public boolean isSelected(String sheetName, Collection<String> selectedSheets) {
        if (ObjectUtils.isEmpty(sheetName) || CollectionUtils.isEmpty(selectedSheets)) {
            return false;
        }
        String key = normalize(sheetName);
        return selectedSheets.stream()
                .filter(s -> !ObjectUtils.isEmpty(s))
                .map(this::normalize)
                .anyMatch(key::equals);
    }

    public List<String> findUnknown(Collection<String> selectedSheets,
                                    Collection<String> availableSheets) {
        if (CollectionUtils.isEmpty(selectedSheets)) {
            return List.of();
        }
        Set<String> available = CollectionUtils.isEmpty(availableSheets)
                ? Set.of()
                : availableSheets.stream()
                        .filter(s -> !ObjectUtils.isEmpty(s))
                        .map(this::normalize)
                        .collect(java.util.stream.Collectors.toSet());

        return selectedSheets.stream()
                .filter(s -> !ObjectUtils.isEmpty(s))
                .filter(s -> !available.contains(normalize(s)))
                .toList();
    }

    public boolean isAllowed(String tableName) {
        return !ObjectUtils.isEmpty(tableName) && ALLOWED_ENTITY_TABLES.contains(normalize(tableName));
    }

    private String normalize(String value) {
        return value.trim().toLowerCase();
    }
}

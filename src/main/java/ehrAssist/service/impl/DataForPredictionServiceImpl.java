package ehrAssist.service.impl;

import ehrAssist.entity.*;
import ehrAssist.entity.master.ObservationCodeMasterEntity;
import ehrAssist.exception.ResourceNotFoundException;
import ehrAssist.repository.*;
import ehrAssist.service.DataForPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DataForPredictionServiceImpl implements DataForPredictionService {

    private final PatientRepository patientRepository;
    private final ObservationRepository observationRepository;
    private final ConditionRepository conditionRepository;
    private final FamilyMemberHistoryRepository familyMemberHistoryRepository;

    // ── Observation codeDisplay → internal key mapping ───────────────────
    // Keys match what the port-481 Fullerton API returns.
    // The map value is a two-element array: [section, fieldName].
    // section: "biometrics" | "bppulse" | "laboratory"
    private static final Map<String, String[]> OBS_CODE_MAP = new LinkedHashMap<>();

    static {
        // Biometrics
        OBS_CODE_MAP.put("Body Height",              new String[]{"biometrics", "height"});
        OBS_CODE_MAP.put("Body Weight",              new String[]{"biometrics", "weight"});
        OBS_CODE_MAP.put("Body Mass Index",          new String[]{"biometrics", "bodyMassIndex"});
        OBS_CODE_MAP.put("Waist Circumference",      new String[]{"biometrics", "waist"});
        // BP / Pulse
        OBS_CODE_MAP.put("Systolic Blood Pressure",  new String[]{"bppulse", "systolicBloodPressure"});
        OBS_CODE_MAP.put("Diastolic Blood Pressure",new String[]{"bppulse", "diastolicBloodPressure"});
        OBS_CODE_MAP.put("Heart Rate",               new String[]{"bppulse", "pulse"});
        // Laboratory
        OBS_CODE_MAP.put("Total Cholesterol",        new String[]{"laboratory", "totalCholesterol"});
        OBS_CODE_MAP.put("HDL Cholesterol",          new String[]{"laboratory", "highDensityLipidCholesterol"});
        OBS_CODE_MAP.put("LDL Cholesterol",          new String[]{"laboratory", "lowDensityLipidCholesterol"});
        OBS_CODE_MAP.put("Cholesterol/HDL Ratio",    new String[]{"laboratory", "totalCholHdlRatio"});
        OBS_CODE_MAP.put("Triglycerides",            new String[]{"laboratory", "triglycerides"});
        OBS_CODE_MAP.put("Glucose",                  new String[]{"laboratory", "glucose"});
        OBS_CODE_MAP.put("Hemoglobin A1c",           new String[]{"laboratory", "hba1c"});
        OBS_CODE_MAP.put("HbA1c",                    new String[]{"laboratory", "hba1c"});
        OBS_CODE_MAP.put("C-Reactive Protein",       new String[]{"laboratory", "wrCRP"});
        OBS_CODE_MAP.put("hs-CRP",                   new String[]{"laboratory", "wrCRP"});
        OBS_CODE_MAP.put("Creatinine",               new String[]{"laboratory", "creatinine"});
        OBS_CODE_MAP.put("eGFR",                     new String[]{"laboratory", "egfr"});
        OBS_CODE_MAP.put("Glomerular Filtration Rate", new String[]{"laboratory", "egfr"});
        OBS_CODE_MAP.put("Albumin",                  new String[]{"laboratory", "albumin"});
        OBS_CODE_MAP.put("Globulin",                 new String[]{"laboratory", "globulin"});
        OBS_CODE_MAP.put("Total Protein",            new String[]{"laboratory", "totalProtein"});
        OBS_CODE_MAP.put("Total Bilirubin",          new String[]{"laboratory", "totalBilirubin"});
        OBS_CODE_MAP.put("A/G Ratio",                new String[]{"laboratory", "agRatio"});
        OBS_CODE_MAP.put("PSA",                      new String[]{"laboratory", "prostateAntigen"});
        OBS_CODE_MAP.put("Prostate Specific Antigen", new String[]{"laboratory", "prostateAntigen"});
        OBS_CODE_MAP.put("AFP",                      new String[]{"laboratory", "afp"});
        OBS_CODE_MAP.put("Alpha-Fetoprotein",        new String[]{"laboratory", "afp"});
        OBS_CODE_MAP.put("CEA",                      new String[]{"laboratory", "cea"});
        OBS_CODE_MAP.put("CA-125",                   new String[]{"laboratory", "ca125"});
        OBS_CODE_MAP.put("CA 19-9",                  new String[]{"laboratory", "ca19"});
        OBS_CODE_MAP.put("CA 15-3",                  new String[]{"laboratory", "ca153"});
        OBS_CODE_MAP.put("AST",                      new String[]{"laboratory", "sgotAst"});
        OBS_CODE_MAP.put("ALT",                      new String[]{"laboratory", "sgptAlt"});
        OBS_CODE_MAP.put("ALP",                      new String[]{"laboratory", "alkalinePhosphatase"});
        OBS_CODE_MAP.put("Alkaline Phosphatase",     new String[]{"laboratory", "alkalinePhosphatase"});
        OBS_CODE_MAP.put("GGT",                      new String[]{"laboratory", "ggt"});
        OBS_CODE_MAP.put("Hemoglobin",               new String[]{"laboratory", "haemoglobin"});
        OBS_CODE_MAP.put("Hematocrit",               new String[]{"laboratory", "haematocrit"});
        OBS_CODE_MAP.put("Platelets",                new String[]{"laboratory", "platelets"});
        OBS_CODE_MAP.put("RDW",                      new String[]{"laboratory", "rdw"});
        OBS_CODE_MAP.put("MCH",                      new String[]{"laboratory", "mch"});
        OBS_CODE_MAP.put("Uric Acid",                new String[]{"laboratory", "uricAcid"});
        OBS_CODE_MAP.put("Urea",                     new String[]{"laboratory", "urea"});
        OBS_CODE_MAP.put("Sodium",                   new String[]{"laboratory", "sodium"});
        OBS_CODE_MAP.put("Potassium",                new String[]{"laboratory", "potassium"});
        OBS_CODE_MAP.put("Chloride",                 new String[]{"laboratory", "chloride"});
        OBS_CODE_MAP.put("Calcium",                  new String[]{"laboratory", "calcium"});
        OBS_CODE_MAP.put("Bicarbonate",              new String[]{"laboratory", "bicarbonate"});
        OBS_CODE_MAP.put("Hepatitis B Surface Antigen", new String[]{"laboratory", "hepatitisBAntigen"});
        OBS_CODE_MAP.put("EBV EA IgA",               new String[]{"laboratory", "ebvEaIga"});
    }

    // Social-history codeDisplay → healthhistory key
    private static final Map<String, String> SOCIAL_HISTORY_MAP = new LinkedHashMap<>();

    static {
        SOCIAL_HISTORY_MAP.put("Tobacco Smoking Status",    "habits-smokingCategory");
        SOCIAL_HISTORY_MAP.put("Smoking Status",            "habits-smokingCategory");
        SOCIAL_HISTORY_MAP.put("Cigarette Smoking",         "habits-cigarettesSmokingCategory");
        SOCIAL_HISTORY_MAP.put("Alcohol Intake",            "habits-alcoholIntake");
        SOCIAL_HISTORY_MAP.put("Alcohol Use",               "habits-alcoholIntake");
        SOCIAL_HISTORY_MAP.put("Exercise",                  "habits-exercise");
        SOCIAL_HISTORY_MAP.put("Physical Activity",         "habits-exercise");
        SOCIAL_HISTORY_MAP.put("Exercise In A Week",        "habits-exerciseInAWeek");
    }

    // ── Condition display → medicalHistory key ──────────────────────────
    private static final Map<String, String> CONDITION_MAP = new LinkedHashMap<>();

    static {
        CONDITION_MAP.put("hypertension",           "medicalHistory-hypertensionDiagnosed");
        CONDITION_MAP.put("high blood pressure",    "medicalHistory-hypertensionDiagnosed");
        CONDITION_MAP.put("diabetes mellitus type 2", "medicalHistory-diabetes");
        CONDITION_MAP.put("diabetes mellitus type 1", "medicalHistory-diabetesType1");
        CONDITION_MAP.put("diabetes",               "medicalHistory-diabetes");
        CONDITION_MAP.put("chronic kidney",         "medicalHistory-chronicKidneyDisease");
        CONDITION_MAP.put("renal disease",          "medicalHistory-chronicKidneyDisease");
        CONDITION_MAP.put("high cholesterol",       "medicalHistory-highCholesterol");
        CONDITION_MAP.put("hyperlipidemia",         "medicalHistory-highCholesterol");
        CONDITION_MAP.put("dyslipidemia",           "medicalHistory-highCholesterol");
        CONDITION_MAP.put("obesity",                "medicalHistory-obesity");
        CONDITION_MAP.put("fatty liver",            "medicalHistory-fattyLiver");
        CONDITION_MAP.put("nafld",                  "medicalHistory-nafld");
        CONDITION_MAP.put("nash",                   "medicalHistory-fattyLiver");
        CONDITION_MAP.put("asthma",                 "medicalHistory-asthma");
        CONDITION_MAP.put("depression",             "medicalHistory-depression");
        CONDITION_MAP.put("anxiety",                "medicalHistory-anxiety");
        CONDITION_MAP.put("stroke",                 "medicalHistory-stroke");
        CONDITION_MAP.put("heart disease",          "medicalHistory-heartDisease");
        CONDITION_MAP.put("heart attack",           "medicalHistory-heartAttack");
        CONDITION_MAP.put("congestive heart failure", "medicalHistory-congestiveHeartFailure");
        CONDITION_MAP.put("thyroid",                "medicalHistory-thyroidProblem");
        CONDITION_MAP.put("gout",                   "medicalHistory-gout");
        CONDITION_MAP.put("gallstones",             "medicalHistory-gallstones");
        CONDITION_MAP.put("kidney stone",           "medicalHistory-kidneyStone");
        CONDITION_MAP.put("lupus",                  "medicalHistory-lupus");
        CONDITION_MAP.put("thalassemia",            "medicalHistory-thalassemia");
        CONDITION_MAP.put("rheumatoid arthritis",   "medicalHistory-rheumatoidArthritis");
        CONDITION_MAP.put("breast cancer",          "medicalHistory-breastCancer");
        CONDITION_MAP.put("colon cancer",           "medicalHistory-colonCancer");
        CONDITION_MAP.put("lung cancer",            "medicalHistory-lungCancer");
        CONDITION_MAP.put("melanoma",               "medicalHistory-melanomaSkin");
        CONDITION_MAP.put("nasopharyngeal",         "medicalHistory-nasopharyngrealCancer");
        CONDITION_MAP.put("ovarian cancer",         "medicalHistory-ovarianCancer");
        CONDITION_MAP.put("prostate cancer",        "medicalHistory-prostateCancer");
        CONDITION_MAP.put("uterine cancer",         "medicalHistory-uterineCancer");
        CONDITION_MAP.put("peripheral vascular",    "medicalHistory-peripheralVascularDisease");
        CONDITION_MAP.put("polycystic ovarian",     "medicalHistory-polycysticOvarianSyndrome");
        CONDITION_MAP.put("pcos",                   "medicalHistory-polycysticOvarianSyndrome");
        CONDITION_MAP.put("gestational diabetes",   "medicalHistory-gestationalDiabetes");
        CONDITION_MAP.put("endometriosis",          "medicalHistory-endometriosis");
        CONDITION_MAP.put("hepatitis b",            "medicalHistory-hepatitisBCarrier");
    }

    // ── Family relationship code → prefix ───────────────────────────────
    private static final Map<String, String> RELATIONSHIP_PREFIX = new LinkedHashMap<>();

    static {
        RELATIONSHIP_PREFIX.put("FTH",  "family-father-");
        RELATIONSHIP_PREFIX.put("MTH",  "family-mother-");
        RELATIONSHIP_PREFIX.put("SIB",  "family-siblings-");
        RELATIONSHIP_PREFIX.put("NSIS", "family-siblings-");
        RELATIONSHIP_PREFIX.put("NBRO", "family-siblings-");
        RELATIONSHIP_PREFIX.put("SIS",  "family-siblings-");
        RELATIONSHIP_PREFIX.put("BRO",  "family-siblings-");
    }

    // Family condition display → suffix
    private static final Map<String, String> FAMILY_CONDITION_SUFFIX = new LinkedHashMap<>();

    static {
        FAMILY_CONDITION_SUFFIX.put("heart disease",        "heartDisease");
        FAMILY_CONDITION_SUFFIX.put("high blood pressure",  "highBloodPressure");
        FAMILY_CONDITION_SUFFIX.put("hypertension",         "highBloodPressure");
        FAMILY_CONDITION_SUFFIX.put("high cholesterol",     "highCholesterol");
        FAMILY_CONDITION_SUFFIX.put("hyperlipidemia",       "highCholesterol");
        FAMILY_CONDITION_SUFFIX.put("stroke",               "stroke");
        FAMILY_CONDITION_SUFFIX.put("diabetes",             "diabetes");
        FAMILY_CONDITION_SUFFIX.put("obesity",              "obesity");
        FAMILY_CONDITION_SUFFIX.put("fatty liver",          "fattyLiver");
        FAMILY_CONDITION_SUFFIX.put("chronic kidney",       "chronicKidneyDisease");
        FAMILY_CONDITION_SUFFIX.put("nasopharyngeal",       "nasopharyngrealCancer");
        FAMILY_CONDITION_SUFFIX.put("breast cancer",        "breastCancer");
        FAMILY_CONDITION_SUFFIX.put("lung cancer",          "lungCancer");
        FAMILY_CONDITION_SUFFIX.put("ovarian cancer",       "ovarianCancer");
        FAMILY_CONDITION_SUFFIX.put("uterine cancer",       "uterineCancer");
        FAMILY_CONDITION_SUFFIX.put("colon cancer",         "colonCancer");
        FAMILY_CONDITION_SUFFIX.put("prostate cancer",      "prostateCancer");
        FAMILY_CONDITION_SUFFIX.put("melanoma",             "melanomaSkin");
        FAMILY_CONDITION_SUFFIX.put("cystic fibrosis",      "cysticFibrosis");
        FAMILY_CONDITION_SUFFIX.put("haemophilia",          "haemophilia");
        FAMILY_CONDITION_SUFFIX.put("thalassemia",          "thalassemia");
        FAMILY_CONDITION_SUFFIX.put("lupus",                "lupus");
        FAMILY_CONDITION_SUFFIX.put("rheumatoid arthritis", "rheumatoidArthritis");
        FAMILY_CONDITION_SUFFIX.put("depression",           "depression");
        FAMILY_CONDITION_SUFFIX.put("schizophrenia",        "depressionSchizophrenia");
        FAMILY_CONDITION_SUFFIX.put("asthma",               "asthma");
        FAMILY_CONDITION_SUFFIX.put("anxiety",              "anxiety");
        FAMILY_CONDITION_SUFFIX.put("thyroid",              "thyroidProblem");
        FAMILY_CONDITION_SUFFIX.put("hepatitis b",          "hepatitisBCarrier");
        FAMILY_CONDITION_SUFFIX.put("gout",                 "gout");
        FAMILY_CONDITION_SUFFIX.put("gallstones",           "gallstones");
        FAMILY_CONDITION_SUFFIX.put("polycystic ovarian",   "polycysticOvarianSyndrome");
        FAMILY_CONDITION_SUFFIX.put("endometriosis",        "endometriosis");
        FAMILY_CONDITION_SUFFIX.put("breast lumps",         "breastLumps");
        FAMILY_CONDITION_SUFFIX.put("fibroids",             "fibroids");
    }

    // ── All default medicalHistory keys (value "No") ─────────────────────
    private static final List<String> ALL_MEDICAL_HISTORY_KEYS = List.of(
            "medicalHistory-hypertensionDiagnosed", "medicalHistory-diabetes",
            "medicalHistory-diabetesType1", "medicalHistory-chronicKidneyDisease",
            "medicalHistory-highCholesterol", "medicalHistory-obesity",
            "medicalHistory-fattyLiver", "medicalHistory-nafld",
            "medicalHistory-asthma", "medicalHistory-depression",
            "medicalHistory-anxiety", "medicalHistory-stroke",
            "medicalHistory-heartDisease", "medicalHistory-heartAttack",
            "medicalHistory-congestiveHeartFailure", "medicalHistory-thyroidProblem",
            "medicalHistory-gout", "medicalHistory-gallstones",
            "medicalHistory-kidneyStone", "medicalHistory-lupus",
            "medicalHistory-thalassemia", "medicalHistory-rheumatoidArthritis",
            "medicalHistory-breastCancer", "medicalHistory-colonCancer",
            "medicalHistory-lungCancer", "medicalHistory-melanomaSkin",
            "medicalHistory-nasopharyngrealCancer", "medicalHistory-ovarianCancer",
            "medicalHistory-prostateCancer", "medicalHistory-uterineCancer",
            "medicalHistory-peripheralVascularDisease",
            "medicalHistory-polycysticOvarianSyndrome",
            "medicalHistory-gestationalDiabetes", "medicalHistory-endometriosis",
            "medicalHistory-hepatitisBCarrier"
    );

    // ── All default family-* suffixes ───────────────────────────────────
    private static final List<String> ALL_FAMILY_SUFFIXES = List.of(
            "heartDisease", "highBloodPressure", "highCholesterol", "stroke",
            "diabetes", "obesity", "fattyLiver", "chronicKidneyDisease",
            "nasopharyngrealCancer", "breastCancer", "lungCancer",
            "ovarianCancer", "uterineCancer", "colonCancer", "prostateCancer",
            "melanomaSkin", "cysticFibrosis", "haemophilia", "thalassemia",
            "lupus", "rheumatoidArthritis", "learningDisabilities",
            "depression", "depressionSchizophrenia", "asthma", "anxiety",
            "thyroidProblem", "hepatitisBCarrier", "gout", "gallstones"
    );

    private static final List<String> FAMILY_PREFIXES = List.of(
            "family-father-", "family-mother-",
            "family-siblings-", "family-otherRelatives-"
    );

    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> buildPredictionData(UUID patientId) {
        PatientEntity patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        List<ObservationEntity> observations = observationRepository.findByPatientId(patientId);
        List<ConditionEntity> conditions = conditionRepository.findByPatientId(patientId);
        List<FamilyMemberHistoryEntity> familyHistories = familyMemberHistoryRepository.findByPatientId(patientId);

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("patientName", buildPatientName(patient));
        result.put("gender", capitalize(patient.getGender()));
        result.put("dob", patient.getBirthDate() != null ? patient.getBirthDate().toString() : "");

        Map<String, Object> section = new LinkedHashMap<>();
        section.put("biometrics", Map.of("data", buildObservationSection(observations, "biometrics")));
        section.put("bppulse", Map.of("data", buildObservationSection(observations, "bppulse")));
        section.put("laboratory", Map.of("data", buildObservationSection(observations, "laboratory")));
        section.put("healthhistory", Map.of("data", buildHealthHistory(observations, conditions, familyHistories)));
        result.put("section", section);

        return result;
    }

    // ── Demographics ─────────────────────────────────────────────────────

    private String buildPatientName(PatientEntity patient) {
        if (patient.getNames() == null || patient.getNames().isEmpty()) {
            return "Unknown";
        }
        PatientNameEntity name = patient.getNames().get(0);
        String given = name.getGivenFirst() != null ? name.getGivenFirst() : "";
        String family = name.getFamily() != null ? name.getFamily() : "";
        return (given + " " + family).trim();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // ── Observations → biometrics / bppulse / laboratory sections ────────

    private Map<String, Object> buildObservationSection(List<ObservationEntity> observations, String targetSection) {
        Map<String, Object> data = new LinkedHashMap<>();

        for (ObservationEntity obs : observations) {
            ObservationCodeMasterEntity code = obs.getCodeMaster();
            if (code == null || code.getCodeDisplay() == null) continue;

            String[] mapping = resolveObsMapping(code.getCodeDisplay());
            if (mapping == null || !mapping[0].equals(targetSection)) continue;

            String fieldName = mapping[1];
            if (obs.getValueQuantity() != null) {
                String unit = obs.getValueUnit();
                if (unit == null) unit = code.getExpectedUnit();
                data.put(fieldName, buildValueMap(obs.getValueQuantity().toPlainString(), unit));
            } else if (obs.getValueString() != null) {
                data.put(fieldName, obs.getValueString());
            }
        }

        return data;
    }

    private String[] resolveObsMapping(String codeDisplay) {
        String[] exact = OBS_CODE_MAP.get(codeDisplay);
        if (exact != null) return exact;
        String lower = codeDisplay.toLowerCase();
        for (Map.Entry<String, String[]> entry : OBS_CODE_MAP.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<String, String> buildValueMap(String value, String unit) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("value", value != null ? value : "");
        m.put("unit", unit != null ? unit : "");
        return m;
    }

    // ── Health history (conditions + social history + family) ────────────

    private Map<String, Object> buildHealthHistory(
            List<ObservationEntity> observations,
            List<ConditionEntity> conditions,
            List<FamilyMemberHistoryEntity> familyHistories) {

        Map<String, Object> hh = new LinkedHashMap<>();

        // 1. Default all medicalHistory keys to "No"
        for (String key : ALL_MEDICAL_HISTORY_KEYS) {
            hh.put(key, "No");
        }

        // 2. Set active conditions to "Yes"
        for (ConditionEntity cond : conditions) {
            String display = conditionDisplay(cond);
            if (display == null) continue;
            String lower = display.toLowerCase();
            for (Map.Entry<String, String> entry : CONDITION_MAP.entrySet()) {
                if (lower.contains(entry.getKey())) {
                    hh.put(entry.getValue(), "Yes");
                }
            }
        }

        // 3. Social-history observations → habits-* keys
        for (ObservationEntity obs : observations) {
            ObservationCodeMasterEntity code = obs.getCodeMaster();
            if (code == null || code.getFhirCategoryCode() == null) continue;
            if (!"social-history".equals(code.getFhirCategoryCode())) continue;

            String hhKey = resolveSocialHistoryKey(code.getCodeDisplay());
            if (hhKey == null) continue;

            if (obs.getValueString() != null) {
                hh.put(hhKey, obs.getValueString());
            } else if (obs.getValueQuantity() != null) {
                Map<String, String> valMap = buildValueMap(
                        obs.getValueQuantity().toPlainString(),
                        obs.getValueUnit() != null ? obs.getValueUnit() : code.getExpectedUnit());
                hh.put(hhKey, valMap);
            }
        }

        // 4. Default all family-* keys to "No"
        for (String prefix : FAMILY_PREFIXES) {
            for (String suffix : ALL_FAMILY_SUFFIXES) {
                hh.put(prefix + suffix, "No");
            }
        }

        // 5. Set family conditions to "Yes"
        for (FamilyMemberHistoryEntity fmh : familyHistories) {
            String prefix = resolveRelationshipPrefix(fmh.getRelationshipCode(), fmh.getRelationshipDisplay());

            for (FamilyMemberHistoryConditionEntity fc : fmh.getConditions()) {
                String condDisplay = fc.getCodeDisplay();
                if (condDisplay == null) continue;
                String suffix = resolveFamilySuffix(condDisplay);
                if (suffix != null) {
                    hh.put(prefix + suffix, "Yes");
                }
            }
        }

        return hh;
    }

    private String conditionDisplay(ConditionEntity cond) {
        if (cond.getCodeMaster() != null) {
            if (cond.getCodeMaster().getLongTitle() != null) return cond.getCodeMaster().getLongTitle();
            if (cond.getCodeMaster().getShortTitle() != null) return cond.getCodeMaster().getShortTitle();
        }
        return null;
    }

    private String resolveSocialHistoryKey(String codeDisplay) {
        if (codeDisplay == null) return null;
        String exact = SOCIAL_HISTORY_MAP.get(codeDisplay);
        if (exact != null) return exact;
        String lower = codeDisplay.toLowerCase();
        for (Map.Entry<String, String> entry : SOCIAL_HISTORY_MAP.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String resolveRelationshipPrefix(String code, String display) {
        if (code != null) {
            String prefix = RELATIONSHIP_PREFIX.get(code.toUpperCase());
            if (prefix != null) return prefix;
        }
        if (display != null) {
            String lower = display.toLowerCase();
            if (lower.contains("father")) return "family-father-";
            if (lower.contains("mother")) return "family-mother-";
            if (lower.contains("sister") || lower.contains("brother") || lower.contains("sibling"))
                return "family-siblings-";
        }
        return "family-otherRelatives-";
    }

    private String resolveFamilySuffix(String condDisplay) {
        String lower = condDisplay.toLowerCase();
        for (Map.Entry<String, String> entry : FAMILY_CONDITION_SUFFIX.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}

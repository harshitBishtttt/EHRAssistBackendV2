package ehrAssist.audit;

import ehrAssist.entity.FhirAuditEventEntity;
import ehrAssist.repository.FhirAuditEventRepository;
import ehrAssist.security.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Spring MVC interceptor that writes one audit row per HTTP API call into
 * {@code fhir_audit_event}.
 * <p>
 * Design contracts:
 * <ul>
 *     <li>Runs in {@link #afterCompletion} — audit work never delays the client response.</li>
 *     <li>All logic is wrapped in try/catch — an audit failure MUST NOT break the API.</li>
 *     <li>Null-safe for every optional value (auth, headers, path vars, query, patient id).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "ehrAssist.audit.startTimeMs";
    private static final String BASE_FHIR_PREFIX = "/baseR4/";
    private static final int MAX_PATH_LEN = 500;
    private static final int MAX_QUERY_LEN = 1000;
    private static final int MAX_RESOURCE_TYPE_LEN = 50;
    private static final int MAX_RESOURCE_ID_LEN = 64;
    private static final int MAX_AGENT_ID_LEN = 128;
    private static final int MAX_AGENT_EMAIL_LEN = 255;

    private final FhirAuditEventRepository auditRepository;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        try {
            if (shouldSkip(request)) {
                return;
            }
            FhirAuditEventEntity entity = buildAuditEntity(request, response, ex);
            persistSafely(entity);
        } catch (Throwable t) {
            log.error("Audit interceptor failure (suppressed): {}", t.getMessage(), t);
        }
    }

    private boolean shouldSkip(HttpServletRequest request) {
        if (request == null) {
            return true;
        }
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        String path = request.getRequestURI();
        if (ObjectUtils.isEmpty(path)) {
            return true;
        }
        return path.startsWith("/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.equals("/baseR4/metadata");
    }

    private FhirAuditEventEntity buildAuditEntity(HttpServletRequest request,
                                                  HttpServletResponse response,
                                                  Exception ex) {
        int status = response != null ? response.getStatus() : 0;
        String httpMethod = safe(request.getMethod(), "UNKNOWN");

        String resourceType = extractResourceType(request);
        String resourceId = extractResourceId(request);
        UUID patientId = extractPatientId(request, resourceType, resourceId);

        return FhirAuditEventEntity.builder()
                .recorded(LocalDateTime.now())
                .action(mapAction(httpMethod))
                .outcome(mapOutcome(status, ex))
                .agentUserId(truncate(safe(extractAgentUserId(), "anonymous"), MAX_AGENT_ID_LEN))
                .agentUserEmail(truncate(extractAgentEmail(), MAX_AGENT_EMAIL_LEN))
                .agentIp(extractClientIp(request))
                .httpMethod(httpMethod)
                .requestPath(truncate(request.getRequestURI(), MAX_PATH_LEN))
                .requestQuery(truncate(request.getQueryString(), MAX_QUERY_LEN))
                .responseStatus(status)
                .executionTimeMs(computeElapsedMs(request))
                .resourceType(truncate(resourceType, MAX_RESOURCE_TYPE_LEN))
                .resourceId(truncate(resourceId, MAX_RESOURCE_ID_LEN))
                .patientId(patientId)
                .build();
    }

    private void persistSafely(FhirAuditEventEntity entity) {
        try {
            auditRepository.save(entity);
        } catch (Exception e) {
            log.error("Audit row save failed (suppressed): method={} path={} status={} err={}",
                    entity.getHttpMethod(),
                    entity.getRequestPath(),
                    entity.getResponseStatus(),
                    e.getMessage(), e);
        }
    }

    /**
     * Maps HTTP verb to FHIR AuditEvent.action code.
     * C=Create, R=Read, U=Update, D=Delete, E=Execute/other.
     */
    private String mapAction(String method) {
        if (ObjectUtils.isEmpty(method)) {
            return "E";
        }
        switch (method.toUpperCase()) {
            case "POST":
                return "C";
            case "GET":
            case "HEAD":
                return "R";
            case "PUT":
            case "PATCH":
                return "U";
            case "DELETE":
                return "D";
            default:
                return "E";
        }
    }

    /**
     * Maps HTTP status / exception presence to FHIR AuditEvent.outcome code.
     * 0=Success, 4=MinorFailure, 8=SeriousFailure, 12=MajorFailure.
     */
    private String mapOutcome(int status, Exception ex) {
        if (ex != null) {
            return "8";
        }
        if (status >= 200 && status < 400) {
            return "0";
        }
        if (status >= 400 && status < 500) {
            return "4";
        }
        if (status >= 500) {
            return "8";
        }
        return "4";
    }

    private String extractAgentUserId() {
        try {
            return AuthUtils.currentUid();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractAgentEmail() {
        try {
            return AuthUtils.currentEmail();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        try {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (!ObjectUtils.isEmpty(forwarded)) {
                int comma = forwarded.indexOf(',');
                String first = (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
                if (!ObjectUtils.isEmpty(first)) {
                    return first.length() > 45 ? first.substring(0, 45) : first;
                }
            }
            String remote = request.getRemoteAddr();
            if (!ObjectUtils.isEmpty(remote)) {
                return remote.length() > 45 ? remote.substring(0, 45) : remote;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Integer computeElapsedMs(HttpServletRequest request) {
        try {
            Object start = request.getAttribute(START_TIME_ATTR);
            if (start instanceof Long) {
                long elapsed = System.currentTimeMillis() - (Long) start;
                if (elapsed < 0) {
                    return 0;
                }
                return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) elapsed;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * For paths beginning with {@code /baseR4/}, returns the first segment after
     * the prefix (e.g. {@code Patient}, {@code Observation}). Non-FHIR paths return null.
     */
    private String extractResourceType(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (ObjectUtils.isEmpty(path) || !path.startsWith(BASE_FHIR_PREFIX)) {
            return null;
        }
        String remainder = path.substring(BASE_FHIR_PREFIX.length());
        int slash = remainder.indexOf('/');
        String segment = slash < 0 ? remainder : remainder.substring(0, slash);
        if (ObjectUtils.isEmpty(segment)) {
            return null;
        }
        return segment;
    }

    /**
     * Resolves resource id from (in order):
     * path vars {@code id} / {@code resourceId}, then FHIR search params
     * {@code _id} / {@code id}. Returns null if nothing present.
     */
    @SuppressWarnings("unchecked")
    private String extractResourceId(HttpServletRequest request) {
        try {
            Map<String, String> vars = (Map<String, String>) request.getAttribute(
                    HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (!ObjectUtils.isEmpty(vars)) {
                String fromPath = firstNonEmpty(vars.get("id"), vars.get("resourceId"));
                if (!ObjectUtils.isEmpty(fromPath)) {
                    return fromPath;
                }
            }
            String fromQuery = firstNonEmpty(
                    request.getParameter("_id"),
                    request.getParameter("id")
            );
            return ObjectUtils.isEmpty(fromQuery) ? null : fromQuery;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Resolves patient id from (in priority order):
     * <ol>
     *     <li>path var {@code patientId}</li>
     *     <li>query params {@code patientId}, {@code patient_id}, {@code patient}, {@code subject}</li>
     *     <li>if resource type is {@code Patient} and a resource id was captured, use that</li>
     * </ol>
     * Returns null if nothing parseable as UUID is found.
     */
    @SuppressWarnings("unchecked")
    private UUID extractPatientId(HttpServletRequest request,
                                  String resourceType,
                                  String resourceId) {
        try {
            Map<String, String> vars = (Map<String, String>) request.getAttribute(
                    HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (!ObjectUtils.isEmpty(vars)) {
                UUID fromPath = parseUuidQuietly(vars.get("patientId"));
                if (fromPath != null) {
                    return fromPath;
                }
            }
            String[] keys = {"patientId", "patient_id", "patient", "subject"};
            for (String key : keys) {
                UUID parsed = parseUuidQuietly(request.getParameter(key));
                if (parsed != null) {
                    return parsed;
                }
            }
            if ("Patient".equalsIgnoreCase(resourceType) && !ObjectUtils.isEmpty(resourceId)) {
                UUID mirrored = parseUuidQuietly(resourceId);
                if (mirrored != null) {
                    return mirrored;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (!ObjectUtils.isEmpty(v)) {
                return v;
            }
        }
        return null;
    }

    private UUID parseUuidQuietly(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String safe(String value, String fallback) {
        return ObjectUtils.isEmpty(value) ? fallback : value;
    }
}

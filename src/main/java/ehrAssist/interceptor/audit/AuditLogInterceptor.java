package ehrAssist.interceptor.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.nio.charset.StandardCharsets;
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
    private static final int MAX_RESPONSE_BODY_BYTES = 512_000;

    private final FhirAuditEventRepository auditRepository;
    private final ObjectMapper objectMapper;

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
        return "/api/v1/users/login".equals(path)
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

        JsonNode parsedBody = null;
        if (ObjectUtils.isEmpty(resourceId) || patientId == null) {
            parsedBody = parseResponseBody(response, status);
        }
        if (ObjectUtils.isEmpty(resourceId) && parsedBody != null) {
            resourceId = extractResourceIdFromBody(parsedBody, resourceType);
        }
        if (patientId == null && parsedBody != null) {
            patientId = extractPatientIdFromBody(parsedBody);
        }

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

    private JsonNode parseResponseBody(HttpServletResponse response, int status) {
        if (response == null || status < 200 || status >= 300) {
            return null;
        }
        try {
            ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(
                    response, ContentCachingResponseWrapper.class);
            if (wrapper == null) {
                return null;
            }
            byte[] bytes = wrapper.getContentAsByteArray();
            if (bytes == null || bytes.length == 0 || bytes.length > MAX_RESPONSE_BODY_BYTES) {
                return null;
            }
            String contentType = wrapper.getContentType();
            if (!ObjectUtils.isEmpty(contentType)
                    && !contentType.toLowerCase().contains("json")) {
                return null;
            }
            return objectMapper.readTree(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    private String extractResourceIdFromBody(JsonNode root, String expectedType) {
        try {
            if (root == null) {
                return null;
            }
            String rootType = text(root, "resourceType");
            if (!ObjectUtils.isEmpty(rootType)
                    && !"Bundle".equalsIgnoreCase(rootType)
                    && (ObjectUtils.isEmpty(expectedType) || expectedType.equalsIgnoreCase(rootType))) {
                String id = text(root, "id");
                return ObjectUtils.isEmpty(id) ? null : id;
            }
            if ("Bundle".equalsIgnoreCase(rootType)) {
                JsonNode firstResource = firstEntryResource(root, expectedType);
                if (firstResource != null) {
                    String id = text(firstResource, "id");
                    return ObjectUtils.isEmpty(id) ? null : id;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private UUID extractPatientIdFromBody(JsonNode root) {
        try {
            if (root == null) {
                return null;
            }
            String rootType = text(root, "resourceType");
            if ("Patient".equalsIgnoreCase(rootType)) {
                return parseUuidQuietly(text(root, "id"));
            }
            if ("Bundle".equalsIgnoreCase(rootType)) {
                JsonNode firstPatient = firstEntryResource(root, "Patient");
                if (firstPatient != null) {
                    UUID id = parseUuidQuietly(text(firstPatient, "id"));
                    if (id != null) {
                        return id;
                    }
                }
            }
            UUID fromSubject = parseUuidQuietly(
                    stripPatientReferencePrefix(textAt(root, "subject", "reference")));
            if (fromSubject != null) {
                return fromSubject;
            }
            UUID fromPatient = parseUuidQuietly(
                    stripPatientReferencePrefix(textAt(root, "patient", "reference")));
            if (fromPatient != null) {
                return fromPatient;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private JsonNode firstEntryResource(JsonNode bundleRoot, String expectedType) {
        JsonNode entries = bundleRoot.get("entry");
        if (entries == null || !entries.isArray()) {
            return null;
        }
        for (JsonNode entry : entries) {
            JsonNode resource = entry.get("resource");
            if (resource == null) {
                continue;
            }
            if (ObjectUtils.isEmpty(expectedType)
                    || expectedType.equalsIgnoreCase(text(resource, "resourceType"))) {
                return resource;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode child = node.get(field);
        return (child == null || child.isNull()) ? null : child.asText(null);
    }

    private String textAt(JsonNode node, String first, String second) {
        if (node == null) {
            return null;
        }
        JsonNode child = node.get(first);
        return child == null ? null : text(child, second);
    }

    private String stripPatientReferencePrefix(String reference) {
        if (ObjectUtils.isEmpty(reference)) {
            return null;
        }
        String prefix = "Patient/";
        int idx = reference.indexOf(prefix);
        return idx >= 0 ? reference.substring(idx + prefix.length()) : reference;
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

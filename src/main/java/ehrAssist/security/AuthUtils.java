package ehrAssist.security;

import ehrAssist.exception.FhirValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Reads identity set by {@link ehrAssist.security.jwt.JwtAuthFilter} ({@code jwtUserId} attribute,
 * email as {@link Authentication#getPrincipal()}).
 */
@Slf4j
public final class AuthUtils {

    private static final String JWT_USER_ID_ATTR = "jwtUserId";

    private AuthUtils() {
    }

    /**
     * Authenticated user id from JWT, or {@code null} if absent.
     */
    public static UUID currentUserId() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        Object attr = request.getAttribute(JWT_USER_ID_ATTR);
        return attr instanceof UUID u ? u : null;
    }

    /**
     * Same as {@link #currentUserId()} as string (e.g. for audit columns).
     */
    public static String currentUid() {
        UUID id = currentUserId();
        return id != null ? id.toString() : null;
    }

    /**
     * Email from the JWT-backed {@link Authentication}, or {@code null}.
     */
    public static String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        return principal instanceof String s ? s : null;
    }

    public static String requireCurrentEmail() {
        String email = currentEmail();
        if (ObjectUtils.isEmpty(email)) {
            log.warn("No authenticated email found on current request");
            throw new FhirValidationException("Authenticated user email not found on request");
        }
        return email;
    }

    /**
     * Display name is not carried in the JWT; returns {@code null}.
     */
    public static String currentName() {
        return null;
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attributes).getRequest();
        }
        return null;
    }
}

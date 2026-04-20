package ehrAssist.security;

import com.google.firebase.auth.FirebaseToken;
import ehrAssist.exception.FhirValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Utility for extracting authenticated user details from the current request.
 * <p>
 * Relies on {@link FirebaseAuthFilter} having populated the request attribute
 * {@code "firebaseUser"} with a verified {@link FirebaseToken}.
 * <p>
 * Safe to call from controllers, services, aspects, or interceptors within an
 * active HTTP request thread.
 */
@Slf4j
public final class AuthUtils {

    private static final String FIREBASE_USER_ATTR = "firebaseUser";

    private AuthUtils() {
    }

    /**
     * Returns the verified Firebase token attached to the current request,
     * or {@code null} if no request is active or the token is absent.
     */
    public static FirebaseToken currentToken() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        Object attr = request.getAttribute(FIREBASE_USER_ATTR);
        return attr instanceof FirebaseToken ? (FirebaseToken) attr : null;
    }

    /**
     * Returns the authenticated user's email, or {@code null} if unavailable.
     * Prefer {@link #requireCurrentEmail()} when the email is mandatory.
     */
    public static String currentEmail() {
        FirebaseToken token = currentToken();
        return token != null ? token.getEmail() : null;
    }

    /**
     * Returns the authenticated user's email and throws if unavailable.
     * Use this in business logic where an authenticated caller is required.
     */
    public static String requireCurrentEmail() {
        String email = currentEmail();
        if (ObjectUtils.isEmpty(email)) {
            log.warn("No authenticated email found on current request");
            throw new FhirValidationException("Authenticated user email not found on request");
        }
        return email;
    }

    /**
     * Returns the authenticated user's Firebase UID, or {@code null} if unavailable.
     */
    public static String currentUid() {
        FirebaseToken token = currentToken();
        return token != null ? token.getUid() : null;
    }

    /**
     * Returns the authenticated user's display name, or {@code null} if unavailable.
     */
    public static String currentName() {
        FirebaseToken token = currentToken();
        return token != null ? token.getName() : null;
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attributes).getRequest();
        }
        return null;
    }
}

package ehrAssist.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Wraps the outgoing {@link HttpServletResponse} with
 * {@link ContentCachingResponseWrapper} so {@link AuditLogInterceptor} can
 * inspect the response body (FHIR JSON) during {@code afterCompletion} to
 * extract resource ids / patient ids when URL-based extraction is insufficient.
 * <p>
 * The original response bytes are always copied back at the end — client-facing
 * behaviour is unchanged.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class AuditResponseCachingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return path.startsWith("/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.equals("/baseR4/metadata");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrapped);
        } finally {
            wrapped.copyBodyToResponse();
        }
    }
}

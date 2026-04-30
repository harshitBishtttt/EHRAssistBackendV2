package ehrAssist.interceptor.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link AuditLogInterceptor} globally.
 * <p>
 * Excludes unauthenticated login, docs, and metadata — little HIPAA audit value.
 */
@Configuration
@RequiredArgsConstructor
public class AuditWebMvcConfig implements WebMvcConfigurer {

    private final AuditLogInterceptor auditLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditLogInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/v1/users/login",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/**",
                        "/baseR4/metadata"
                )
                .order(0);
    }
}

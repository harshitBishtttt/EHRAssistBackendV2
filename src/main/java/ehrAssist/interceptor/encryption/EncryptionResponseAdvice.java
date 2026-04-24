package ehrAssist.interceptor.encryption;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;

/**
 * Transparently encrypts every API response body using AES-256-GCM when
 * {@code app.encryption.enabled=true} in application.properties.
 * <p>
 * When {@code app.encryption.enabled=false} (default), this advice is
 * completely inert — the {@link #supports} method returns false and
 * Spring skips it entirely. Zero overhead, zero impact.
 * <p>
 * Works at the Spring MVC level, so no servlet filter ordering conflicts.
 * Controllers remain completely unaware of encryption.
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class EncryptionResponseAdvice implements ResponseBodyAdvice<Object> {

    private final EncryptionProperties encryptionProperties;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return encryptionProperties.isEnabled();
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body == null) {
            return null;
        }

        try {
            String plaintext = body instanceof String ? (String) body : objectMapper.writeValueAsString(body);
            String encrypted = AesGcmEncryptionUtil.encrypt(plaintext, encryptionProperties.getKey());

            Map<String, Object> envelope = Map.of("encrypted", true, "payload", encrypted);
            String json = objectMapper.writeValueAsString(envelope);

            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            if (body instanceof String) {
                return json;
            }
            return envelope;
        } catch (Exception e) {
            log.error("Response encryption failed, returning original body: {}", e.getMessage(), e);
            return body;
        }
    }
}

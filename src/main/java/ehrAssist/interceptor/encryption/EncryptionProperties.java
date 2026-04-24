package ehrAssist.interceptor.encryption;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.encryption")
public class EncryptionProperties {

    private boolean enabled = false;

    private String key;
}

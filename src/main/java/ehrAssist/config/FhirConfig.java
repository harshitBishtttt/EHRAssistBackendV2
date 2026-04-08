package ehrAssist.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;

@Configuration
public class FhirConfig {

    @Value("${custom.ssl.trust-store}")
    private Resource trustStoreResource;

    @Value("${custom.ssl.trust-store-password}")
    private String trustStorePassword;

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    public RestTemplate restTemplate() throws Exception {
        SSLContext sslContext = buildSslContext();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                if (connection instanceof HttpsURLConnection httpsConn) {
                    httpsConn.setSSLSocketFactory(sslContext.getSocketFactory());
                }
                super.prepareConnection(connection, httpMethod);
            }
        };

        return new RestTemplate(factory);
    }

    private SSLContext buildSslContext() throws Exception {
        KeyStore customTs = KeyStore.getInstance("PKCS12");
        try (InputStream is = trustStoreResource.getInputStream()) {
            customTs.load(is, trustStorePassword.toCharArray());
        }

        X509TrustManager defaultTm = getX509TrustManager(null);
        X509TrustManager customTm = getX509TrustManager(customTs);

        X509TrustManager compositeTm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws java.security.cert.CertificateException {
                defaultTm.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws java.security.cert.CertificateException {
                try {
                    customTm.checkServerTrusted(chain, authType);
                } catch (java.security.cert.CertificateException e) {
                    defaultTm.checkServerTrusted(chain, authType);
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                X509Certificate[] defaults = defaultTm.getAcceptedIssuers();
                X509Certificate[] custom = customTm.getAcceptedIssuers();
                X509Certificate[] merged = Arrays.copyOf(defaults, defaults.length + custom.length);
                System.arraycopy(custom, 0, merged, defaults.length, custom.length);
                return merged;
            }
        };

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[]{compositeTm}, null);
        return ctx;
    }

    private X509TrustManager getX509TrustManager(KeyStore keyStore) throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager x509Tm) {
                return x509Tm;
            }
        }
        throw new IllegalStateException("No X509TrustManager found");
    }
}

package africa.flot.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

@ApplicationScoped
public class NoopHostnameVerifier implements HostnameVerifier {
    @Override
    public boolean verify(String hostname, SSLSession session) {
        return true; // Attention: À utiliser uniquement en développement ou avec des certificats auto-signés
    }
}
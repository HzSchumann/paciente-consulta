package br.com.techchallenge.historico.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security")
public class ApplicationSecurityProperties {

    private final Jwt jwt = new Jwt();

    public Jwt getJwt() {
        return jwt;
    }

    public static class Jwt {
        private String issuer;
        private String publicKey;
        private String jwkSetUri;
        private String tokenStatusUri;
        private String internalSecret;
        private Duration ttl = Duration.ofHours(1);

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        public String getTokenStatusUri() {
            return tokenStatusUri;
        }

        public void setTokenStatusUri(String tokenStatusUri) {
            this.tokenStatusUri = tokenStatusUri;
        }

        public String getInternalSecret() {
            return internalSecret;
        }

        public void setInternalSecret(String internalSecret) {
            this.internalSecret = internalSecret;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }
}

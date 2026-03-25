package br.com.techchallenge.agendamento.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({ApplicationSecurityProperties.class, OutboxPublisherProperties.class})
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    RestClient restClient() {
        return RestClient.builder().build();
    }

    @Bean
    JwtDecoder jwtDecoder(ApplicationSecurityProperties properties, RestClient restClient) {
        NimbusJwtDecoder decoder = properties.getJwt().getJwkSetUri() != null && !properties.getJwt().getJwkSetUri().isBlank()
                ? NimbusJwtDecoder.withJwkSetUri(properties.getJwt().getJwkSetUri()).build()
                : NimbusJwtDecoder.withPublicKey(parsePublicKey(properties.getJwt().getPublicKey()))
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        OAuth2TokenValidatorResult success = OAuth2TokenValidatorResult.success();
        OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(properties.getJwt().getIssuer());
        OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> revocationValidator = jwt -> {
            if (properties.getJwt().getTokenStatusUri() == null || properties.getJwt().getTokenStatusUri().isBlank()) {
                return success;
            }
            Map<?, ?> response = restClient.get()
                    .uri(properties.getJwt().getTokenStatusUri() + "/" + jwt.getId())
                    .header("X-Internal-Secret", properties.getJwt().getInternalSecret())
                    .retrieve()
                    .body(Map.class);
            if (Boolean.TRUE.equals(response.get("revoked"))) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("revoked_token", "Token revogado", null));
            }
            return success;
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, revocationValidator));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthorityPrefix("ROLE_");
        converter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
        return authenticationConverter;
    }

    private static RSAPublicKey parsePublicKey(String value) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(normalize(value));
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Chave publica JWT invalida", ex);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Configuracao JWT ausente");
        }
        return value
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }
}

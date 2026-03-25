package br.com.techchallenge.agendamento;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

final class JwtTestTokens {

    private static final String TEST_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDs8ugFV7MjUHY0bSYDhcPCKw7wZcX4NueJ3IjIer3JD1zqLiBx5nF81YBvddLeldYpCazLolVFCOewinQe4zzqrldQICIk6L1/KGhkq+VG28QDimif1GXKGO9RfXRr22hCCqxVWqqB/J4Wcu1eCkfx9Q0jS6G2bv+Gwg57kHLjoJDuRXCtdrwDWhCpKLpMhSGtNCvUg6hNKYWd9QgUXESvmYxVug3KN6wlhnvWJZilmjmJBNwlca8uLUAQ8cd+GDWo/3CYC2WRwuWNHFtJsgD/Pc2JWw3XVe+5M0qe65FSC74NialOGJ4z8ezCED6uSCOpkCMA7npVepsilCO2xYZlAgMBAAECggEBAK1hlNuL773we34jPASp1bN3tGe94N2et1AOelxQTB0aS7/j3sPZfsN8qo1kptdOxrWiqbkb4M8yE+7/cLbFSmbjCWrNCI7/auHn3HOFwLBX0RdSKPqC/bSjCEMVzKG7m49vpeiS/l89TSRaFkyQs1JeIK3qAruufvXJe7V43kimZdYmgvoKemhAWv4WoUeawdKRln3qyK30jtiuKMPlL1K07oGqKATIgc/mWuBtrYSJZQpozsS3drZO8mF4AeNCETydxQW6NgUf41YbYVHQmFVrL5jGBGXibNOg+UkyAOkTujZ83UiBLEjub4VprTUa5YEE69yTcgfyySudCVfqaCUCgYEA9tPLsUoodY5GjjjsZvXcPMha9TNKgIycDTYtqQn3XB8xnvAkP3xLusRez9ZGqz9H/HWKjB9rk67a3Yt3ALB1Msg9fZn3IkFb+M7xvFQ6s+rLctqzsyYK0M8/DOIUjGqWHoEFQt96p81RHnE3l1mte5LtQIB0q1hJdyLGkmpXGRsCgYEA9cEhnGh+AL7TnRiAiBHB8zLuH5N92IhZbY0gMuXIu/UffuEduhwZ+j/piOlo0n5KdSEyuBzskQcE02dXtQZylaQoIdror/wdktRI5HJrw594Yu9+yDAO/cM0QkuymFk+I+2Hx8/lJ9wrToLvU9yEhUWxAroMU6Ds00VP+QRzVn8CgYBXzVWrNncYAkmE9CjcI19528fHa7x36AznAjvR4vK0OYB8Sm6Kituhj5Mumcm/xx4DZ4imedLZ0RyYLLFjdGHsc6C0Gl34OInQA0Woucm8EKzYqYW9F65HQ0Hpgk7O+Gd4aGsHs9m88WOG0b11wHfe+pAdHtTh0uEnWR6MHPnouwKBgQC1fFDj/s6+oi4IX6Xb1rFSdpQfwX44QEk6e/QBYmxhUd5Whsy7OjF8+2Htd+GvQRCfe0rHaKTTXUmVDUZaK07Hb5krl0wrWpZEbnz+J/mpV8VB2vfXwpXvlQ6svtY/Z/hZ8/pH9yGV1CQvMMapObqr6RGVn+umfrsmMpOipETA5QKBgAbTYVDGpjlCqJNUTzFgGeZPeKO9RAxSWwf9s/6RBIcdBEdie6SZ+PFFAhz3Q9CQDCRVbS5cgM8HBXScS0aQw5b/SFycXC4MUthM8z9xL9D+4hSYamcUBD8DbDUPJD/vq563YPkOJx9xDPw6VMXaWbQDd+6TnvSIhxVxNd1/lGj5";

    private JwtTestTokens() {
    }

    static String bearer(String username, String... roles) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer("techchallenge-paciente-consulta")
                    .subject(username)
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .claim("roles", Arrays.asList(roles))
                    .build();

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .type(JOSEObjectType.JWT)
                            .keyID("test-key")
                            .build(),
                    claims
            );
            jwt.sign(new RSASSASigner(privateKey()));
            return "Bearer " + jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel gerar token de teste", ex);
        }
    }

    private static RSAPrivateKey privateKey() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(TEST_PRIVATE_KEY);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}

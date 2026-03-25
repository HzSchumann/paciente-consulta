package br.com.techchallenge.auth.controller;

import br.com.techchallenge.auth.config.ApplicationSecurityProperties;
import br.com.techchallenge.auth.dto.AuthRequest;
import br.com.techchallenge.auth.dto.AuthResponse;
import br.com.techchallenge.auth.dto.RefreshTokenRequest;
import br.com.techchallenge.auth.dto.RevokeTokenRequest;
import br.com.techchallenge.auth.entity.RefreshTokenSession;
import br.com.techchallenge.auth.service.JwtTokenService;
import br.com.techchallenge.auth.service.JpaUserDetailsService;
import br.com.techchallenge.auth.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final JpaUserDetailsService userDetailsService;
    private final ApplicationSecurityProperties properties;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            JpaUserDetailsService userDetailsService,
            ApplicationSecurityProperties properties
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
        this.properties = properties;
    }

    @PostMapping("/token")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse token(@Valid @RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            JwtTokenService.IssuedToken issuedToken = jwtTokenService.issue(authentication);
            String refreshToken = refreshTokenService.issue(
                    authentication.getName(),
                    Instant.now().plus(properties.getJwt().getRefreshTtl()),
                    issuedToken.jti()
            );
            return new AuthResponse(issuedToken.accessToken(), refreshToken);
        } catch (BadCredentialsException ex) {
            throw ex;
        }
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenSession session = refreshTokenService.consume(request.refreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(session.getUsername());
        JwtTokenService.IssuedToken issuedToken = jwtTokenService.issue(
                userDetails.getUsername(),
                userDetails.getAuthorities()
        );
        String refreshToken = refreshTokenService.issue(
                session.getUsername(),
                Instant.now().plus(properties.getJwt().getRefreshTtl()),
                issuedToken.jti()
        );
        session.setReplacedByTokenId(refreshToken.split("\\.", 2)[0]);
        return new AuthResponse(issuedToken.accessToken(), refreshToken);
    }

    @PostMapping("/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@RequestBody(required = false) RevokeTokenRequest request, Authentication authentication) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            refreshTokenService.revokeByRefreshToken(request.refreshToken());
        }
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            refreshTokenService.revokeAccessToken(jwt.getId(), jwt.getExpiresAt());
        }
    }
}

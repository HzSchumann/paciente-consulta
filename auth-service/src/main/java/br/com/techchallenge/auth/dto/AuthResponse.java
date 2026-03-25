package br.com.techchallenge.auth.dto;

public record AuthResponse(String accessToken, String refreshToken) {
}

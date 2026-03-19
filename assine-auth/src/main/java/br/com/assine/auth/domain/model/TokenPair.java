package br.com.assine.auth.domain.model;

public record TokenPair(String accessToken, String refreshToken) {
}

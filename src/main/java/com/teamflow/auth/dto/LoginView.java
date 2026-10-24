package com.teamflow.auth.dto;

public record LoginView(String accessToken, String tokenType, long expiresInSeconds) {}

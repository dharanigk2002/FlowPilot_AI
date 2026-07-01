package com.flowpilot.auth;

public record CsrfTokenResponse(String token, String headerName) {
}

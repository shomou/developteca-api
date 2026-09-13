package com.developteca.dto;

public class LoginResponse {

    private String token;
    private String refreshToken;
    private UserResponse user;
    private String tokenType = "Bearer";
    
    // ============= CONSTRUCTORES =============

    public LoginResponse() {}

    public LoginResponse(String token, String refreshToken, UserResponse user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    // ============= GETTERS Y SETTERS =============

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}

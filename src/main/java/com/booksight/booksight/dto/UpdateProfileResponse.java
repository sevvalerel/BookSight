package com.booksight.booksight.dto;

public class UpdateProfileResponse {

    private UserDTO user;
    private String token;

    public UpdateProfileResponse(UserDTO user, String token) {
        this.user = user;
        this.token = token;
    }

    public UserDTO getUser() { return user; }
    public String getToken() { return token; }
}

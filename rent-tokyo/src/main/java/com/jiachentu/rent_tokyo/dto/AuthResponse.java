package com.jiachentu.rent_tokyo.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private Long userId;
    private String email;
    private String displayName;
    private String token;
}

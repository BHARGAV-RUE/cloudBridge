package com.bhargav.cloudbridge.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class AdminLoginResponse {

    private String token;
    private String username;
    private LocalDateTime expiresAt;
}
package com.example.aitourism.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GuestSessionResponse {

    private String token;
    private Long expiresIn;
    private String sessionId;
    private Long expiresAt;
    private GuestUser user;

    @Data
    @AllArgsConstructor
    public static class GuestUser {
        private String userId;
        private String nickname;
        private boolean guest;
        private boolean persistenceAuthorized;
    }
}

package com.khi.securityservice.core.entity.security;

import lombok.Data;

@Data
public class SecurityUserPrincipalEntity {

    private String uid;
    private String nickname;
    private String role;
}

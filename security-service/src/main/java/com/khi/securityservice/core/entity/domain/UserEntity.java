package com.khi.securityservice.core.entity.domain;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String uid;
    private String role;
//    private String nickname;
//    private String name;
}

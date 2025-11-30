package com.khi.securityservice.core.controller.dto;

import com.khi.securityservice.core.entity.domain.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponseDto {
    private String userId;
    private String nickname;
    private String profileImageUrl;

    public UserProfileResponseDto(UserEntity user) {
        this.userId = user.getUid();
        this.nickname = user.getNickname();
        this.profileImageUrl = user.getProfileImgUrl();
    }
}

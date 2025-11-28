package com.khi.securityservice.core.util;

import com.khi.securityservice.core.controller.dto.UserInfo;
import com.khi.securityservice.core.entity.domain.UserEntity;
import com.khi.securityservice.core.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserUtil {

    private final UserRepository userRepository;

    public UserUtil(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserInfo getUserInfo(String userId) {
        UserEntity userEntity = userRepository.findByUid(userId);
        return UserInfo.builder()
                .role(userEntity.getRole())
                .nickname("userEntity.getNickname()")
                .userId(userId)
                .name("userEntity.getName()")
                .build();
    }

    public List<UserInfo> getUserInfos(List<String> userIds) {
        return userIds.stream()
                .map(this::getUserInfo)
                .collect(Collectors.toList());
    }
}

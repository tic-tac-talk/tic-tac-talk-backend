package com.khi.securityservice.core.service;

import com.khi.securityservice.core.controller.dto.UserProfileResponseDto;
import com.khi.securityservice.core.entity.domain.UserEntity;
import com.khi.securityservice.core.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final NcpStorageService ncpStorageService;
    private final int NICKNAME_MAX_CHAR = 8;

    public UserService(UserRepository userRepository, NcpStorageService ncpStorageService) {
        this.userRepository = userRepository;
        this.ncpStorageService = ncpStorageService;
    }
    // 유저 프로필 수정
    @Transactional
    public UserProfileResponseDto updateUserProfile(String userId, String nickname, MultipartFile image, boolean isProfileImageDeleted) {
        UserEntity user = userRepository.findOptionalByUid(userId)
                .orElseThrow(() -> new IllegalArgumentException("User Not found"));


        if (nickname != null && !nickname.isBlank()) {
            if (nickname.length() > NICKNAME_MAX_CHAR) {
                throw new IllegalArgumentException("Nickname must be 6 characters or fewer");
            }
            user.setNickname(nickname);

            log.info("[SECURITY-SERVICE] userId: {} 변경된 닉네임: {}", userId, nickname);
        }

        if (image != null && !image.isEmpty()) {
            deleteCurrentProfileImage(user);

            String imageUrl = ncpStorageService.uploadFile(image);
            user.setProfileImgUrl(imageUrl);

            log.info("[SECURITY-SERVICE] userId: {} 프로필 이미지 변경", userId);
        } else if (isProfileImageDeleted) {
            deleteCurrentProfileImage(user);
            user.setProfileImgUrl(null);
        }
        userRepository.save(user);

        return new UserProfileResponseDto(user);
    }
    // 유저 프로필 조회
    public UserProfileResponseDto getUserProfile(String userId) {
        UserEntity user = userRepository.findOptionalByUid(userId)
                .orElseThrow(() -> new RuntimeException("User Not found"));
        log.info("[SECURITY-SERVICE] userId: {} 프로필 조회", userId);

        return new UserProfileResponseDto(user);
    }
    // 기존 프로필 이미지 Object Storage에서 삭제
    public void deleteCurrentProfileImage(UserEntity user) {

        if (user.getProfileImgUrl() != null) {
            ncpStorageService.deleteFile(user.getProfileImgUrl());
            log.info("[SECURITY-SERVICE] userId: {} 프로필 이미지 Object Storage에서 삭제", user.getId());
        }
    }
}

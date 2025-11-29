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

    public UserService(UserRepository userRepository, NcpStorageService ncpStorageService) {
        this.userRepository = userRepository;
        this.ncpStorageService = ncpStorageService;
    }

    @Transactional
    public UserProfileResponseDto updateUserProfile(String userId, String nickname, MultipartFile image, boolean isProfileImageDeleted) {
        UserEntity user = userRepository.findOptionalByUid(userId)
                .orElseThrow(() -> new RuntimeException("User Not found"));


        if (nickname != null && !nickname.isBlank()) {
            user.setNickname(nickname);
            log.info("nickname 변경");
        }

        if (image != null && !image.isEmpty()) {
            String imageUrl = ncpStorageService.uploadFile(image);
            user.setProfileImgUrl(imageUrl);
        } else if (isProfileImageDeleted) {
            user.setProfileImgUrl(null);
        }
        userRepository.save(user);

        return new UserProfileResponseDto(user);
    }
}

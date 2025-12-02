package com.khi.securityservice.core.service;

import com.khi.securityservice.core.entity.domain.UserEntity;
import com.khi.securityservice.core.entity.security.SecurityUserPrincipalEntity;
import com.khi.securityservice.core.principal.SecurityUserPrincipal;
import com.khi.securityservice.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final NicknameService nicknameService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User authUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        if (registrationId.equals("kakao")) {

            log.info("카카오 소셜 로그인 registrationId 식별");

            return kakaoOAuth2Process(authUser);

        } else {

            log.info("일치하는 소셜 로그인 registrationId가 없음");

            return null;
        }
    }

    private SecurityUserPrincipal kakaoOAuth2Process(OAuth2User authUser) {

        Map<String, Object> attributes = authUser.getAttributes();
        String authUid = attributes.get("id").toString();

        UserEntity existUser = userRepository.findByUid(authUid);

        // 회원가입
        String nickname;
        if (existUser == null) {
            nickname = nicknameService.generateRandomNickname();

            UserEntity userEntity = new UserEntity();
            userEntity.setUid(authUid);
            userEntity.setRole("ROLE_USER");
            userEntity.setNickname(nickname);
            userEntity.setProfileImgUrl(null);
            userRepository.save(userEntity);

            log.info("회원가입 완료");
        } else {
            nickname = existUser.getNickname();
        }

        SecurityUserPrincipalEntity userPrincipalEntity = new SecurityUserPrincipalEntity();
        userPrincipalEntity.setUid(authUid);
        userPrincipalEntity.setNickname(nickname);
        userPrincipalEntity.setRole("ROLE_USER");

        return new SecurityUserPrincipal(userPrincipalEntity);
    }
}

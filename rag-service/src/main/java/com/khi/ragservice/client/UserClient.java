package com.khi.ragservice.client;

import com.khi.ragservice.common.api.ApiResponse;
import com.khi.ragservice.dto.UserInfo;
import com.khi.ragservice.dto.UserNicknameRequestDto;
import com.khi.ragservice.dto.UserProfileResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign client for security-service user profile operations
 */
@FeignClient(name = "security-service", url = "${security-service.url}")
public interface UserClient {

    @PostMapping("/security/feign/user/nickname")
    ApiResponse<UserProfileResponseDto> getUserNickname(@RequestBody UserNicknameRequestDto requestDto);

    @GetMapping("/security/users/{userId}")
    UserInfo getUserInfo(@PathVariable("userId") String userId);

    @GetMapping("/security/users/batch")
    List<UserInfo> getUserInfos(@RequestParam("userIds") List<String> userIds);
}

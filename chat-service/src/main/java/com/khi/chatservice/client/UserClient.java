package com.khi.chatservice.client;

import com.khi.chatservice.client.dto.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserClient {

    @GetMapping("/api/users/{userId}")
    UserInfo getUserInfo(@PathVariable("userId") String userId);

    @GetMapping("/api/users/batch")
    List<UserInfo> getUserInfos(@RequestParam("userIds") List<String> userIds);
}
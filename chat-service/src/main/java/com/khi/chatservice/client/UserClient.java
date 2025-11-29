package com.khi.chatservice.client;

import com.khi.chatservice.client.dto.UserInfo;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Hidden
@FeignClient(name = "security-service", url = "${security-service.url}")
public interface UserClient {

    @GetMapping("/users/{userId}")
    UserInfo getUserInfo(@PathVariable("userId") String userId);

    @GetMapping("/users/batch")
    List<UserInfo> getUserInfos(@RequestParam("userIds") List<String> userIds);
}
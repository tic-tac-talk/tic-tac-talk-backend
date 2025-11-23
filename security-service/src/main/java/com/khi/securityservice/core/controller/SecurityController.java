package com.khi.securityservice.core.controller;

import com.khi.securityservice.core.controller.dto.UserInfo;
import com.khi.securityservice.core.util.UserUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/security")
public class SecurityController {
    //다른 모듈에서 유저 정보를 가져오기 위한 컨트롤러로, 외부에 노출되지 않습니다.

    private final UserUtil userUtil;
    public SecurityController(UserUtil userUtil) {
        this.userUtil = userUtil;
    }

    @GetMapping("/users/{userId}")
    public UserInfo getUserInfo(@PathVariable("userId") String userId){
        return userUtil.getUserInfo(userId);
    };

    @GetMapping("/users/batch")
    public List<UserInfo> getUserInfos(@RequestParam("userIds") List<String> userIds){
        return userUtil.getUserInfos(userIds);
    };
}

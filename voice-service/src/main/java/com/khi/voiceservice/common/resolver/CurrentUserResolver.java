package com.khi.voiceservice.common.resolver;

import com.khi.voiceservice.common.dto.UserInfo;
import com.khi.voiceservice.common.annotation.CurrentUser;
import com.khi.voiceservice.common.exception.ApiException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) throws Exception {

        String userId = webRequest.getHeader("X-User-Id");
        String userRole = webRequest.getHeader("X-User-Role");

        if (userId == null || userId.isEmpty()) {
            throw new ApiException("인증 정보가 없습니다.");
        }

        return UserInfo.builder()
                .userId(userId)
                .userRole(userRole)
                .build();
    }
}
package com.khi.ragservice.common.api;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private final ResponseStatus status;

    private final String message;

    private final T data;

    /* 데이터가 있는 성공 응답 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseStatus.SUCCESS, "요청이 정상적으로 처리되었습니다.", data);
    }

    /* 데이터가 없는 성공 응답 */
    public static ApiResponse<?> success() {
        return new ApiResponse<>(ResponseStatus.SUCCESS, "요청이 정상적으로 처리되었습니다.", null);
    }

    /* 검증 실패 응답 */
    public static ApiResponse<?> failure(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();

        List<ObjectError> allErrors = bindingResult.getAllErrors();
        for (ObjectError error : allErrors) {
            if (error instanceof FieldError) {
                errors.put(((FieldError) error).getField(), error.getDefaultMessage());
            } else {
                errors.put(error.getObjectName(), error.getDefaultMessage());
            }
        }
        return new ApiResponse<>(ResponseStatus.FAILURE, "요청 값이 유효하지 않습니다.", errors);
    }

    /* 예외·비즈니스 오류 응답 */
    public static ApiResponse<?> error(String message) {
        return new ApiResponse<>(ResponseStatus.ERROR, message, null);
    }
}

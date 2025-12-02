package com.khi.chatservice.common.exception;

import com.khi.chatservice.common.api.ApiResponse;
import com.khi.chatservice.common.exception.type.ApiException;
import com.khi.chatservice.common.exception.type.ForbiddenException;
import com.khi.chatservice.common.exception.type.NotFoundException;
import com.khi.chatservice.common.exception.type.UnauthorizedException;
import com.khi.chatservice.common.exception.type.WebSocketException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionControllerAdvice {

    /**
     * 400 Bad Request - 비즈니스 로직 에러
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<?>> handleApiException(ApiException exception) {
        log.warn("ApiException: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(exception.getMessage()));
    }

    /**
     * 400 Bad Request - Validation 에러
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException exception) {
        log.warn("Validation error: {}", exception.getMessage());
        BindingResult bindingResult = exception.getBindingResult();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(bindingResult));
    }

    /**
     * 400 Bad Request - Spring Validation 에러
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleSpringValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException exception) {
        log.warn("Spring Validation error: {}", exception.getMessage());
        BindingResult bindingResult = exception.getBindingResult();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(bindingResult));
    }

    /**
     * 401 Unauthorized - 인증 에러
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorizedException(UnauthorizedException exception) {
        log.warn("UnauthorizedException: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(exception.getMessage()));
    }

    /**
     * 403 Forbidden - 권한 에러
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<?>> handleForbiddenException(ForbiddenException exception) {
        log.warn("ForbiddenException: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(exception.getMessage()));
    }

    /**
     * 404 Not Found - 리소스 없음
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFoundException(NotFoundException exception) {
        log.warn("NotFoundException: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(exception.getMessage()));
    }

    /**
     * 500 Internal Server Error - WebSocket 에러
     */
    @ExceptionHandler(WebSocketException.class)
    public ResponseEntity<ApiResponse<?>> handleWebSocketException(WebSocketException exception) {
        log.error("WebSocketException: {}", exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("WebSocket 통신 중 오류가 발생했습니다: " + exception.getMessage()));
    }

    /**
     * 500 Internal Server Error - 기타 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception exception) {
        log.error("Unhandled exception: {}", exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<?>> handleJwtException(JwtException exception) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(exception.getMessage()));
    }
}

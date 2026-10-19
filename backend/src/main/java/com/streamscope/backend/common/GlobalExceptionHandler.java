package com.streamscope.backend.common;

import feign.FeignException;
import feign.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleBusinessError(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        String code = switch (status) {
            case 400 -> "INVALID_REQUEST";
            case 401 -> "AUTH_REQUIRED";
            case 403 -> "ACCESS_DENIED";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 502 -> "UPSTREAM_ERROR";
            case 503 -> "SERVICE_UNAVAILABLE";
            default -> "REQUEST_FAILED";
        };
        String message = exception.getReason() == null
                ? "请求无法完成" : exception.getReason();
        return error(status, code, message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        var field = exception.getBindingResult().getFieldError();
        String message = field == null || field.getDefaultMessage() == null
                ? "输入参数不符合要求" : field.getDefaultMessage();
        return error(400, "INVALID_REQUEST", message);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleInvalidRequest(Exception exception) {
        return error(400, "INVALID_REQUEST", "请求参数缺失或格式不正确");
    }

    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<ApiError> handleConnectionError(RetryableException exception) {
        log.warn("Twitch 请求连接失败或超时，类型={}", exception.getClass().getSimpleName());
        return error(503, "TWITCH_UNAVAILABLE", "暂时无法连接 Twitch，请稍后重试");
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiError> handleTwitchError(FeignException exception) {
        int status = exception.status();
        log.warn("Twitch 请求失败，HTTP 状态码={}", status);
        if (status == 429) {
            return error(503, "TWITCH_RATE_LIMITED", "Twitch 请求受到限流，请稍后重试");
        }
        if (status == 401 || status == 403) {
            return error(502, "TWITCH_AUTH_ERROR", "Twitch 服务授权失败，请稍后重试");
        }
        return error(502, "TWITCH_UPSTREAM_ERROR", "Twitch 服务暂时无法完成请求，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedError(Exception exception) {
        // 保留 Spring 框架自身的 HTTP 状态，例如 404、405 和 415。
        if (exception instanceof ErrorResponse frameworkError) {
            return ResponseEntity.status(frameworkError.getStatusCode())
                    .headers(frameworkError.getHeaders())
                    .body(new ApiError("INVALID_REQUEST", "请求无法处理，请检查路径、方法和参数"));
        }
        log.error("未处理的服务异常", exception);
        return error(500, "INTERNAL_ERROR", "服务内部异常，请稍后重试");
    }

    private ResponseEntity<ApiError> error(int status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }
}

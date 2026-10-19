package com.streamscope.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Pattern(
                regexp = "[A-Za-z0-9_]{3,32}",
                message = "用户名必须为 3 到 32 位字母、数字或下划线"
        )
        String username,

        @NotBlank
        @Size(
                min = 8,
                max = 72,
                message = "密码长度必须为 8 到 72 个字符"
        )
        String password
) {
}
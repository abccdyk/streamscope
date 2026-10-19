package com.streamscope.backend.twitch;

import java.util.function.Function;

import feign.FeignException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TwitchRequestExecutor {

    private static final Logger log =
            LoggerFactory.getLogger(TwitchRequestExecutor.class);

    private final TwitchTokenService tokenService;

    public TwitchRequestExecutor(TwitchTokenService tokenService) {
        this.tokenService = tokenService;
    }

    public <T> T execute(Function<String, T> request) {
        String token = tokenService.getAccessToken();

        try {
            return request.apply(token);
        } catch (FeignException exception) {
            if (exception.status() != 401) {
                throw exception;
            }

            log.warn("Twitch 拒绝了应用 Token，更新后重试一次");

            tokenService.invalidateIfCurrent(token);
        }

        String newToken = tokenService.getAccessToken();

        // 第二次请求放在上面的 catch 外面，不会继续循环重试
        return request.apply(newToken);
    }
}
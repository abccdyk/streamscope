package com.streamscope.backend.twitch;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class TwitchTokenService {

    private final TwitchAuthClient authClient;
    private final String clientId;
    private final String clientSecret;

    private String accessToken;
    private Instant refreshAt = Instant.EPOCH;

    public TwitchTokenService(
            TwitchAuthClient authClient,
            @Value("${twitch.client-id}") String clientId,
            @Value("${twitch.client-secret}") String clientSecret
    ) {
        this.authClient = authClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String getAccessToken() {
        if (accessToken != null && Instant.now().isBefore(refreshAt)) {
            return accessToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "client_credentials");

        Instant requestedAt = Instant.now();
        TwitchTokenResponse response = authClient.getToken(form);

        if (response == null
                || response.accessToken() == null
                || response.accessToken().isBlank()
                || response.expiresIn() <= 0) {
            throw new IllegalStateException("Twitch 返回了无效的 Token 数据");
        }

        accessToken = response.accessToken();
        refreshAt = requestedAt.plusSeconds(
                Math.max(0, response.expiresIn() - 60)
        );

        return accessToken;
    }
    public synchronized void invalidateIfCurrent(String rejectedToken) {
        if (rejectedToken != null && rejectedToken.equals(accessToken)) {
            accessToken = null;
            refreshAt = Instant.EPOCH;
        }
    }
}
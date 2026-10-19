package com.streamscope.backend.twitch;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TwitchTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("token_type") String tokenType
) {
}
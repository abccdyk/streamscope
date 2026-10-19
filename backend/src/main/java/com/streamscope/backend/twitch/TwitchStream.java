package com.streamscope.backend.twitch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TwitchStream(
        String id,
        @JsonProperty("user_login") String userLogin,
        @JsonProperty("user_name") String userName,
        @JsonProperty("game_id") String gameId,
        String title,
        @JsonProperty("viewer_count") int viewerCount,
        @JsonProperty("thumbnail_url") String thumbnailUrl
) {
}
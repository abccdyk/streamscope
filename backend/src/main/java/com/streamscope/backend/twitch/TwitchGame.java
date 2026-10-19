package com.streamscope.backend.twitch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TwitchGame(
        String id,
        String name,
        @JsonProperty("box_art_url") String boxArtUrl
) {
}
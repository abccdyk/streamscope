package com.streamscope.backend.twitch;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TwitchGamesResponse(
        List<TwitchGame> data
) {
}
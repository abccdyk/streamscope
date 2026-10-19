package com.streamscope.backend.recommendation;

import java.util.List;

import com.streamscope.backend.twitch.TwitchStream;

public record RecommendationResponse(
        String mode,
        List<Item> items
) {

    public record Item(
            TwitchStream stream,
            String reason
    ) {
    }
}
package com.streamscope.backend.twitch;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "twitchApi",
        url = "https://api.twitch.tv/helix"
)
public interface TwitchClient {

    @GetMapping("/games/top")
    TwitchGamesResponse getTopGames(
            @RequestHeader("Client-Id") String clientId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam("first") int first
    );
    @GetMapping("/search/categories")
    TwitchGamesResponse searchGames(
            @RequestHeader("Client-Id") String clientId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam("query") String query,
            @RequestParam("first") int first
    );
    @GetMapping("/streams")
    TwitchStreamsResponse getStreams(
            @RequestHeader("Client-Id") String clientId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam("game_id") String gameId,
            @RequestParam("first") int first
    );
    @GetMapping("/games")
    TwitchGamesResponse getGamesById(
            @RequestHeader("Client-Id") String clientId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam("id") String gameId
    );
    @GetMapping("/streams")
    TwitchStreamsResponse getTopStreams(
            @RequestHeader("Client-Id") String clientId,
            @RequestHeader("Authorization") String authorization,
            @RequestParam("first") int first
    );
}
package com.streamscope.backend.game;

import java.util.List;

import com.streamscope.backend.twitch.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.cache.annotation.Cacheable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class GameService {
    private static final Logger log =
            LoggerFactory.getLogger(GameService.class);
    private final TwitchClient twitchClient;
    private final TwitchRequestExecutor requestExecutor;
    private final String clientId;

    public GameService(
            TwitchClient twitchClient,
            TwitchRequestExecutor requestExecutor,
            @Value("${twitch.client-id}") String clientId
    ) {
        this.twitchClient = twitchClient;
        this.requestExecutor = requestExecutor;
        this.clientId = clientId;
    }
    @Cacheable(cacheNames = "topGames", sync = true)
    public List<TwitchGame> getTopGames(int first) {
        log.info("缓存未命中：查询热门游戏，first={}", first);
        TwitchGamesResponse response = requestExecutor.execute(
                token -> twitchClient.getTopGames(
                        clientId,
                        "Bearer " + token,
                        first
                )
        );

        if (response == null || response.data() == null) {
            throw new IllegalStateException("Twitch 返回的游戏数据格式异常");
        }

        return response.data().stream()
                .map(game -> new TwitchGame(
                        game.id(),
                        game.name(),
                        resizeCover(game.boxArtUrl())
                ))
                .toList();
    }

    private String resizeCover(String url) {
        if (url == null) {
            return null;
        }

        return url.replace("{width}", "285")
                .replace("{height}", "380");
    }
    @Cacheable(cacheNames = "gameSearch", sync = true)
    public List<TwitchGame> searchGames(String query, int first) {
        log.info("缓存未命中：搜索游戏，query={}，first={}", query, first);
        TwitchGamesResponse response = requestExecutor.execute(
                token -> twitchClient.searchGames(
                        clientId,
                        "Bearer " + token,
                        query,
                        first
                )
        );

        if (response == null || response.data() == null) {
            throw new IllegalStateException("Twitch 返回的游戏数据格式异常");
        }

        return response.data().stream()
                .map(game -> new TwitchGame(
                        game.id(),
                        game.name(),
                        resizeCover(game.boxArtUrl())
                ))
                .toList();
    }
}
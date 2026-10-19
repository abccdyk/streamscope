package com.streamscope.backend.recommendation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.streamscope.backend.favorite.FavoriteGame;
import com.streamscope.backend.favorite.FavoriteService;
import com.streamscope.backend.recommendation.RecommendationResponse.Item;
import com.streamscope.backend.stream.StreamService;
import com.streamscope.backend.twitch.TwitchStream;

@Service
public class RecommendationService {

    private static final int MAX_GAMES = 5;
    private static final int STREAMS_PER_GAME = 4;
    private static final int MAX_RESULTS = 20;

    private final FavoriteService favoriteService;
    private final StreamService streamService;

    public RecommendationService(
            FavoriteService favoriteService,
            StreamService streamService
    ) {
        this.favoriteService = favoriteService;
        this.streamService = streamService;
    }

    public RecommendationResponse recommend(String username) {
        List<FavoriteGame> favorites = favoriteService
                .getFavorites(username)
                .stream()
                .limit(MAX_GAMES)
                .toList();

        if (favorites.isEmpty()) {
            return recommendPopular();
        }

        return recommendFromFavorites(favorites);
    }

    private RecommendationResponse recommendPopular() {
        Map<String, Item> results = new LinkedHashMap<>();

        for (TwitchStream stream : streamService.getTopStreams(MAX_RESULTS)) {
            if (isUsable(stream)) {
                results.putIfAbsent(
                        stream.id(),
                        new Item(
                                stream,
                                "你还没有收藏游戏，先看看热门直播"
                        )
                );
            }
        }

        return new RecommendationResponse(
                "popular",
                List.copyOf(results.values())
        );
    }

    private RecommendationResponse recommendFromFavorites(
            List<FavoriteGame> favorites
    ) {
        List<List<TwitchStream>> groups = new ArrayList<>();

        for (FavoriteGame favorite : favorites) {
            List<TwitchStream> streams = streamService.getStreams(
                    favorite.twitchGameId(),
                    STREAMS_PER_GAME
            );

            groups.add(
                    streams.stream()
                            .filter(this::isUsable)
                            .filter(stream -> favorite.twitchGameId()
                                    .equals(stream.gameId()))
                            .limit(STREAMS_PER_GAME)
                            .toList()
            );
        }

        Map<String, Item> results = new LinkedHashMap<>();

        // 每轮从各游戏取一条，避免某个游戏占满列表
        for (int round = 0; round < STREAMS_PER_GAME; round++) {
            for (int index = 0; index < favorites.size(); index++) {
                List<TwitchStream> group = groups.get(index);

                if (round >= group.size()) {
                    continue;
                }

                TwitchStream stream = group.get(round);

                results.putIfAbsent(
                        stream.id(),
                        new Item(
                                stream,
                                "因为你收藏了 " + favorites.get(index).gameName()
                        )
                );
            }
        }

        return new RecommendationResponse(
                "favorites",
                results.values().stream()
                        .limit(MAX_RESULTS)
                        .toList()
        );
    }

    private boolean isUsable(TwitchStream stream) {
        return stream != null
                && stream.id() != null
                && !stream.id().isBlank()
                && stream.userLogin() != null
                && !stream.userLogin().isBlank();
    }
}
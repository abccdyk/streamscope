package com.streamscope.backend.favorite;

import java.util.List;

import com.streamscope.backend.twitch.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.streamscope.backend.auth.UserRepository;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final TwitchClient twitchClient;
    private final TwitchRequestExecutor requestExecutor;
    private final String clientId;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            UserRepository userRepository,
            TwitchClient twitchClient,
            TwitchRequestExecutor requestExecutor,
            @Value("${twitch.client-id}") String clientId
    ) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.twitchClient = twitchClient;
        this.requestExecutor = requestExecutor;
        this.clientId = clientId;
    }

    public List<FavoriteGame> getFavorites(String username) {
        Long userId = getUserId(username);

        return favoriteRepository.findByUserIdOrderByIdDesc(userId);
    }

    public void addFavorite(String username, String gameId) {
        Long userId = getUserId(username);

        if (favoriteRepository.existsByUserIdAndTwitchGameId(
                userId, gameId
        )) {
            return;
        }

        TwitchGamesResponse response = requestExecutor.execute(
                token -> twitchClient.getGamesById(
                        clientId,
                        "Bearer " + token,
                        gameId
                )
        );

        if (response == null || response.data() == null) {
            throw new IllegalStateException("Twitch 返回的游戏数据格式异常");
        }

        TwitchGame game = response.data().stream()
                .filter(item -> gameId.equals(item.id()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "游戏不存在"
                ));

        FavoriteGame favorite = new FavoriteGame(
                null,
                userId,
                game.id(),
                game.name()
        );

        try {
            favoriteRepository.save(favorite);
        } catch (DuplicateKeyException exception) {
            // 另一个并发请求已经完成收藏，视为成功
        }
    }

    @Transactional
    public void removeFavorite(String username, String gameId) {
        Long userId = getUserId(username);

        favoriteRepository.deleteFavorite(userId, gameId);
    }

    private Long getUserId(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "当前用户不存在，请重新登录"
                ))
                .id();
    }
}
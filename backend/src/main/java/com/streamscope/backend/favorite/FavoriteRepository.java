package com.streamscope.backend.favorite;

import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository
        extends CrudRepository<FavoriteGame, Long> {

    List<FavoriteGame> findByUserIdOrderByIdDesc(Long userId);

    boolean existsByUserIdAndTwitchGameId(
            Long userId,
            String twitchGameId
    );

    @Modifying
    @Query("""
            DELETE FROM favorite_games
            WHERE user_id = :userId
              AND twitch_game_id = :gameId
            """)
    int deleteFavorite(
            @Param("userId") Long userId,
            @Param("gameId") String gameId
    );
}
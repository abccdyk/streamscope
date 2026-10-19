package com.streamscope.backend.favorite;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("favorite_games")
public record FavoriteGame(
        @Id Long id,
        @Column("user_id") Long userId,
        @Column("twitch_game_id") String twitchGameId,
        @Column("game_name") String gameName
) {
}
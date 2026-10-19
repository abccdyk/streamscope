CREATE TABLE users (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       username VARCHAR(64) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE favorite_games (
                                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                user_id BIGINT NOT NULL,
                                twitch_game_id VARCHAR(64) NOT NULL,
                                game_name VARCHAR(255) NOT NULL,
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(id),
                                CONSTRAINT uq_favorite_game UNIQUE (user_id, twitch_game_id)
);

package com.streamscope.backend.favorite;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/favorites/games")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<FavoriteResponse> getFavorites(
            Authentication authentication
    ) {
        return favoriteService.getFavorites(authentication.getName())
                .stream()
                .map(favorite -> new FavoriteResponse(
                        favorite.twitchGameId(),
                        favorite.gameName()
                ))
                .toList();
    }

    @PutMapping("/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addFavorite(
            @PathVariable("gameId") String gameId,
            Authentication authentication
    ) {
        favoriteService.addFavorite(
                authentication.getName(),
                validateGameId(gameId)
        );
    }

    @DeleteMapping("/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(
            @PathVariable("gameId") String gameId,
            Authentication authentication
    ) {
        favoriteService.removeFavorite(
                authentication.getName(),
                validateGameId(gameId)
        );
    }

    private String validateGameId(String gameId) {
        if (!gameId.matches("[0-9]{1,64}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "gameId 必须是 1 到 64 位数字"
            );
        }

        return gameId;
    }

    public record FavoriteResponse(
            String gameId,
            String gameName
    ) {
    }
}

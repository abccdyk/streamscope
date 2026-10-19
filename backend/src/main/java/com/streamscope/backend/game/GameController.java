package com.streamscope.backend.game;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.streamscope.backend.twitch.TwitchGame;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/top")
    public List<TwitchGame> getTopGames(
            @RequestParam(name = "first", defaultValue = "10") int first
    ) {
        if (first < 1 || first > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "first 必须在 1 到 100 之间"
            );
        }

        return gameService.getTopGames(first);
    }

    @GetMapping("/search")
    public List<TwitchGame> searchGames(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "first", defaultValue = "10") int first
    ) {
        String keyword = query.strip();

        if (keyword.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "搜索关键词不能为空"
            );
        }

        if (keyword.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "搜索关键词不能超过 100 个字符"
            );
        }

        if (first < 1 || first > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "first 必须在 1 到 100 之间"
            );
        }

        return gameService.searchGames(keyword, first);
    }
}
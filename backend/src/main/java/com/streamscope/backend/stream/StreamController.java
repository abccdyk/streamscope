package com.streamscope.backend.stream;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.streamscope.backend.twitch.TwitchStream;

@RestController
@RequestMapping("/api/streams")
public class StreamController {

    private final StreamService streamService;

    public StreamController(StreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping
    public List<TwitchStream> getStreams(
            @RequestParam(name = "gameId") String gameId,
            @RequestParam(name = "first", defaultValue = "10") int first
    ) {
        String normalizedGameId = gameId.strip();

        if (!normalizedGameId.matches("[0-9]{1,64}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "gameId 必须是 1 到 64 位数字"
            );
        }

        if (first < 1 || first > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "first 必须在 1 到 100 之间"
            );
        }

        return streamService.getStreams(normalizedGameId, first);
    }
}
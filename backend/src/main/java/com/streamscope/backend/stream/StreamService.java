package com.streamscope.backend.stream;

import java.util.List;

import com.streamscope.backend.twitch.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.cache.annotation.Cacheable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class StreamService {
    private static final Logger log =
            LoggerFactory.getLogger(StreamService.class);
    private final TwitchClient twitchClient;
    private final TwitchRequestExecutor requestExecutor;
    private final String clientId;

    public StreamService(
            TwitchClient twitchClient,
            TwitchRequestExecutor requestExecutor,
            @Value("${twitch.client-id}") String clientId
    ) {
        this.twitchClient = twitchClient;
        this.requestExecutor = requestExecutor;
        this.clientId = clientId;
    }
    @Cacheable(cacheNames = "gameStreams", sync = true)
    public List<TwitchStream> getStreams(String gameId, int first) {
        log.info("缓存未命中：查询游戏直播，gameId={}，first={}", gameId, first);
        TwitchStreamsResponse response = requestExecutor.execute(
                token -> twitchClient.getStreams(
                        clientId,
                        "Bearer " + token,
                        gameId,
                        first
                )
        );

        return toStreams(response);
    }
    @Cacheable(cacheNames = "topStreams", sync = true)
    public List<TwitchStream> getTopStreams(int first) {
        log.info("缓存未命中：查询热门直播，first={}", first);
        TwitchStreamsResponse response = requestExecutor.execute(
                token -> twitchClient.getTopStreams(
                        clientId,
                        "Bearer " + token,
                        first
                )
        );

        return toStreams(response);
    }

    private List<TwitchStream> toStreams(
            TwitchStreamsResponse response
    ) {
        if (response == null || response.data() == null) {
            throw new IllegalStateException("Twitch 返回的直播数据格式异常");
        }

        return response.data().stream()
                .map(stream -> new TwitchStream(
                        stream.id(),
                        stream.userLogin(),
                        stream.userName(),
                        stream.gameId(),
                        stream.title(),
                        stream.viewerCount(),
                        resizeThumbnail(stream.thumbnailUrl())
                ))
                .toList();
    }

    private String resizeThumbnail(String url) {
        if (url == null) {
            return null;
        }

        return url.replace("{width}", "640")
                .replace("{height}", "360");
    }
}
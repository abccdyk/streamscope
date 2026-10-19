package com.streamscope.backend.twitch;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "twitchAuth",
        url = "https://id.twitch.tv"
)
public interface TwitchAuthClient {

    @PostMapping(
            value = "/oauth2/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    TwitchTokenResponse getToken(
            @RequestBody MultiValueMap<String, String> form
    );
}
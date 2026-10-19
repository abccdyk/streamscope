package com.streamscope.backend.config;

import java.time.Duration;
import java.util.List;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.setAllowNullValues(false);

        // 只允许使用下面明确注册的缓存
        manager.setCacheNames(List.of());

        manager.registerCustomCache(
                "topGames",
                buildCache(Duration.ofMinutes(5), 100)
        );

        manager.registerCustomCache(
                "gameSearch",
                buildCache(Duration.ofMinutes(5), 500)
        );

        manager.registerCustomCache(
                "gameStreams",
                buildCache(Duration.ofSeconds(30), 1000)
        );

        manager.registerCustomCache(
                "topStreams",
                buildCache(Duration.ofSeconds(30), 100)
        );

        return manager;
    }

    private Cache<Object, Object> buildCache(
            Duration ttl,
            long maximumSize
    ) {
        return Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maximumSize)
                .recordStats()
                .build();
    }
}
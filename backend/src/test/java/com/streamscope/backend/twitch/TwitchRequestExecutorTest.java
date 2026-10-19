package com.streamscope.backend.twitch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import feign.FeignException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TwitchRequestExecutorTest {

    @Test
    void successfulRequestDoesNotRefreshToken() {
        TwitchTokenService tokens = mock(TwitchTokenService.class);
        when(tokens.getAccessToken()).thenReturn("valid-token");
        TwitchRequestExecutor executor = new TwitchRequestExecutor(tokens);
        List<String> usedTokens = new ArrayList<>();

        String result = executor.execute(token -> {
            usedTokens.add(token);
            return "success";
        });

        assertEquals("success", result);
        assertEquals(List.of("valid-token"), usedTokens);
        verify(tokens, times(1)).getAccessToken();
        verify(tokens, never()).invalidateIfCurrent(anyString());
    }

    @Test
    void unauthorizedRequestRetriesWithNewToken() {
        TwitchTokenService tokens = mock(TwitchTokenService.class);
        when(tokens.getAccessToken()).thenReturn("old-token", "new-token");
        TwitchRequestExecutor executor = new TwitchRequestExecutor(tokens);
        FeignException unauthorized = upstreamError(401);
        List<String> usedTokens = new ArrayList<>();

        String result = executor.execute(token -> {
            usedTokens.add(token);
            if (usedTokens.size() == 1) {
                throw unauthorized;
            }
            return "success";
        });

        assertEquals("success", result);
        assertEquals(List.of("old-token", "new-token"), usedTokens);
        verify(tokens, times(2)).getAccessToken();
        verify(tokens, times(1)).invalidateIfCurrent("old-token");
        verify(tokens, never()).invalidateIfCurrent("new-token");
    }

    @Test
    void repeatedUnauthorizedResponseStopsAfterOneRetry() {
        TwitchTokenService tokens = mock(TwitchTokenService.class);
        when(tokens.getAccessToken()).thenReturn("old-token", "new-token");
        TwitchRequestExecutor executor = new TwitchRequestExecutor(tokens);
        FeignException unauthorized = upstreamError(401);
        AtomicInteger requests = new AtomicInteger();

        FeignException actual = assertThrows(FeignException.class, () ->
                executor.execute(token -> {
                    requests.incrementAndGet();
                    throw unauthorized;
                })
        );

        assertSame(unauthorized, actual);
        assertEquals(2, requests.get());
        verify(tokens, times(2)).getAccessToken();
        verify(tokens, times(1)).invalidateIfCurrent("old-token");
        verify(tokens, never()).invalidateIfCurrent("new-token");
    }

    @Test
    void otherUpstreamErrorsAreNotRetried() {
        for (int status : new int[]{400, 403, 429, 500, 503, -1}) {
            TwitchTokenService tokens = mock(TwitchTokenService.class);
            when(tokens.getAccessToken()).thenReturn("valid-token");
            TwitchRequestExecutor executor = new TwitchRequestExecutor(tokens);
            FeignException upstreamFailure = upstreamError(status);
            AtomicInteger requests = new AtomicInteger();

            FeignException actual = assertThrows(FeignException.class, () ->
                    executor.execute(token -> {
                        requests.incrementAndGet();
                        throw upstreamFailure;
                    })
            );

            assertSame(upstreamFailure, actual, "status=" + status);
            assertEquals(1, requests.get(), "status=" + status);
            verify(tokens, times(1)).getAccessToken();
            verify(tokens, never()).invalidateIfCurrent(anyString());
        }
    }

    @Test
    void tokenAcquisitionFailureDoesNotExecuteOrRetryQuery() {
        TwitchTokenService tokens = mock(TwitchTokenService.class);
        FeignException authenticationFailure = upstreamError(401);
        when(tokens.getAccessToken()).thenThrow(authenticationFailure);
        TwitchRequestExecutor executor = new TwitchRequestExecutor(tokens);
        AtomicInteger requests = new AtomicInteger();

        FeignException actual = assertThrows(FeignException.class, () ->
                executor.execute(token -> {
                    requests.incrementAndGet();
                    return "should-not-run";
                })
        );

        assertSame(authenticationFailure, actual);
        assertEquals(0, requests.get());
        verify(tokens, times(1)).getAccessToken();
        verify(tokens, never()).invalidateIfCurrent(anyString());
    }

    @Test
    void staleRejectionDoesNotInvalidateAnAlreadyReplacedToken() {
        TwitchAuthClient authClient = mock(TwitchAuthClient.class);
        when(authClient.getToken(any())).thenReturn(
                new TwitchTokenResponse("old-token", 3600, "bearer"),
                new TwitchTokenResponse("new-token", 3600, "bearer")
        );
        TwitchTokenService tokens = new TwitchTokenService(
                authClient, "test-client", "test-secret"
        );

        assertEquals("old-token", tokens.getAccessToken());
        assertEquals("old-token", tokens.getAccessToken());
        tokens.invalidateIfCurrent("old-token");
        assertEquals("new-token", tokens.getAccessToken());

        // 模拟另一个旧请求稍后才收到 401，不应清除已经更新的 Token。
        tokens.invalidateIfCurrent("old-token");
        assertEquals("new-token", tokens.getAccessToken());

        verify(authClient, times(2)).getToken(any());
    }

    private FeignException upstreamError(int status) {
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(status);
        return exception;
    }
}

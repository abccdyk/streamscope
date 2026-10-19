package com.streamscope.backend.common;

import java.util.Map;
import feign.FeignException;
import feign.RetryableException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void conflictUsesStructuredError() throws Exception {
        mvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("用户名已被使用"));
    }

    @Test
    void upstreamAuthorizationFailureIsNotWebsiteLoginFailure() throws Exception {
        for (int code : new int[]{401, 403}) {
            mvc.perform(get("/test/upstream/" + code))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("TWITCH_AUTH_ERROR"));
        }
    }

    @Test
    void upstreamRateLimitUses503() throws Exception {
        mvc.perform(get("/test/upstream/429"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("TWITCH_RATE_LIMITED"));
    }

    @Test
    void upstreamServerFailureUses502() throws Exception {
        mvc.perform(get("/test/upstream/500"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("TWITCH_UPSTREAM_ERROR"));
    }

    @Test
    void connectionErrorUses503() throws Exception {
        mvc.perform(get("/test/connection"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("TWITCH_UNAVAILABLE"));
    }

    @Test
    void validationErrorUses400AndFieldMessage() throws Exception {
        mvc.perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("名称不能为空"));
    }

    @Test
    void malformedJsonUses400() throws Exception {
        mvc.perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void missingOrMalformedQueryParameterUses400() throws Exception {
        mvc.perform(get("/test/number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(get("/test/number").param("value", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void unexpectedErrorDoesNotExposeInternalMessage() throws Exception {
        var result = mvc.perform(get("/test/internal"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andReturn();
        assertFalse(result.getResponse().getContentAsString().contains("internal-secret-detail"));
    }

    @Test
    void wrongHttpMethodPreserves405() throws Exception {
        mvc.perform(post("/test/conflict"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/conflict")
        public void conflict() {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已被使用");
        }

        @GetMapping("/test/upstream/{status}")
        public void upstream(@PathVariable("status") int status) {
            FeignException failure = mock(FeignException.class);
            when(failure.status()).thenReturn(status);
            throw failure;
        }

        @GetMapping("/test/connection")
        public void connection() { throw mock(RetryableException.class); }

        @GetMapping("/test/internal")
        public void internal() { throw new IllegalStateException("internal-secret-detail"); }

        @GetMapping("/test/number")
        public int number(@RequestParam("value") int value) { return value; }

        @PostMapping("/test/validate")
        public Map<String, String> validate(@Valid @RequestBody Input input) {
            return Map.of("name", input.name());
        }
    }

    public record Input(@NotBlank(message = "名称不能为空") String name) { }
}


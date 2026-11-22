package io.github.faridkosarifard.shorturl.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
        properties = {
            "shorturl.rate-limit.enabled=true",
            "shorturl.rate-limit.requests-per-minute=2",
            "shorturl.rate-limit.trust-forwarded-headers=true"
        })
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void limitsWritesPerClientAndSkipsReads() throws Exception {
        String payload = """
                {"url":"https://example.com/rate"}
                """;

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.1")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-RateLimit-Limit", "2"));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.10")
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.10")
                        .content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Too many requests"))
                .andExpect(jsonPath("$.status").value(429));

        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.20")
                        .content(payload))
                .andExpect(status().isCreated());
    }
}

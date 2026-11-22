package io.github.faridkosarifard.shorturl.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.faridkosarifard.shorturl.domain.ShortUrl;
import io.github.faridkosarifard.shorturl.repository.ShortUrlRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "shorturl.rate-limit.enabled=false")
class ShortUrlApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortUrlRepository repository;

    @Test
    void createsShortUrlAndRedirectsWithClickCount() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://example.com/articles/hello"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.url").value("https://example.com/articles/hello"))
                .andExpect(jsonPath("$.clickCount").value(0))
                .andExpect(header().string("Location", containsString("http://localhost:8080/")))
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String code = body.get("code").asText();

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/articles/hello"));

        mockMvc.perform(get("/api/v1/urls/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(1))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/" + code));
    }

    @Test
    void createsCustomAlias() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://example.com/docs","alias":"docs"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("docs"))
                .andExpect(header().string("Location", "http://localhost:8080/docs"));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://example.com/other","alias":"docs"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.title").value("Alias unavailable"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(containsString("already in use")));
    }

    @Test
    void rejectsReservedAliasAndInvalidPayloads() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://example.com","alias":"api"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Alias unavailable"));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"ftp://example.com/file"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid URL"));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"http://127.0.0.1/secret"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Private or local")));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void expiredAndMissingCodesReturnProblemDetails() throws Exception {
        repository.save(new ShortUrl(
                "expired1",
                "https://example.com/old",
                Instant.now().minus(2, ChronoUnit.DAYS),
                Instant.now().minus(1, ChronoUnit.HOURS)));

        mockMvc.perform(get("/expired1"))
                .andExpect(status().isGone())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")))
                .andExpect(jsonPath("$.title").value("Short URL expired"))
                .andExpect(jsonPath("$.status").value(410));

        mockMvc.perform(get("/api/v1/urls/expired1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("expired1"))
                .andExpect(jsonPath("$.clickCount").value(0));

        mockMvc.perform(get("/missing-code"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Short URL not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deletesAShortUrl() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://example.com/tmp","alias":"tmp-link"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/urls/tmp-link")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/urls/tmp-link")).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/urls/tmp-link")).andExpect(status().isNotFound());
    }

    @Test
    void honoursExpiresInDays() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url":"https://example.com/promo","expiresInDays":7}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void exposesOpenApiAndHealth() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("URL Shortener API"))
                .andExpect(jsonPath("$.paths./api/v1/urls").exists());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}

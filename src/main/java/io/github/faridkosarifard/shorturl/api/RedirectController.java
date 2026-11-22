package io.github.faridkosarifard.shorturl.api;

import io.github.faridkosarifard.shorturl.config.ShortUrlProperties;
import io.github.faridkosarifard.shorturl.domain.ShortUrl;
import io.github.faridkosarifard.shorturl.service.ShortUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Redirects")
public class RedirectController {

    private final ShortUrlService service;
    private final ShortUrlProperties properties;

    public RedirectController(ShortUrlService service, ShortUrlProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping("/{code:[A-Za-z0-9_-]{3,64}}")
    @Operation(summary = "Redirect to the target URL")
    @ApiResponse(
            responseCode = "302",
            description = "Redirect to the target URL (301 when configured)",
            headers = @Header(name = "Location", description = "Target URL"),
            content = @Content)
    @ApiResponse(
            responseCode = "404",
            description = "Short URL not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "410",
            description = "Short URL has expired",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        ShortUrl entity = service.resolveForRedirect(code);
        HttpStatus status = HttpStatus.valueOf(properties.redirectStatus());
        return ResponseEntity.status(status).location(URI.create(entity.getTargetUrl())).build();
    }
}

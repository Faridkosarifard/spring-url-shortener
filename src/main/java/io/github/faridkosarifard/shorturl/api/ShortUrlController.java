package io.github.faridkosarifard.shorturl.api;

import io.github.faridkosarifard.shorturl.api.dto.CreateShortUrlRequest;
import io.github.faridkosarifard.shorturl.api.dto.ShortUrlResponse;
import io.github.faridkosarifard.shorturl.service.ShortUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
@Tag(name = "Short URLs")
public class ShortUrlController {

    private final ShortUrlService service;

    public ShortUrlController(ShortUrlService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a short URL")
    @ApiResponse(responseCode = "201", description = "Short URL created")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Alias already in use or reserved",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
        ShortUrlResponse created = service.create(request);
        return ResponseEntity.created(URI.create(created.shortUrl())).body(created);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get short URL details and click count")
    @ApiResponse(responseCode = "200", description = "Short URL found")
    @ApiResponse(
            responseCode = "404",
            description = "Short URL not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ShortUrlResponse get(@PathVariable String code) {
        return service.get(code);
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Delete a short URL")
    @ApiResponse(responseCode = "204", description = "Short URL deleted")
    @ApiResponse(
            responseCode = "404",
            description = "Short URL not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.noContent().build();
    }
}

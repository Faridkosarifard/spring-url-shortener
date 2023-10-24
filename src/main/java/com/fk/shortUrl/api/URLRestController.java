package com.fk.shortUrl.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.fk.shortUrl.common.LinkValidator;
import com.fk.shortUrl.domain.entity.ShortUrlEntity;
import com.fk.shortUrl.domain.view.Views;
import com.fk.shortUrl.service.ShortUrlService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * ShortUrl
 * Created by Farid Kosarifard
 * Date: 10/22/2023
 * Time: 12:23 AM
 */
@RestController
@RequestMapping("/")
@AllArgsConstructor
public class URLRestController {

    private final ShortUrlService service;

    @PostMapping("/url")
    @JsonView(Views.Public.class)
    public Object shortUrl(@RequestBody ShortUrlEntity url){
        if (LinkValidator.getInstance().isLinkValid(url.getFullUrl())) {
            return ResponseEntity.ok("KEY: " + service.createShortUrl(url).getKey());
        }else {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
    }

    @GetMapping("{key}")
    public RedirectView fullUrl(@PathVariable("key") String key){
        return service.getFullUrl(key);
    }
}

package com.fk.shortUrl.web.controller;

import com.fk.shortUrl.common.LinkValidator;
import com.fk.shortUrl.entity.ShortUrlEntity;
import com.fk.shortUrl.service.ShortUrlService;
import com.fk.shortUrl.web.dto.ShortUrlDto;
import org.springframework.beans.factory.annotation.Autowired;
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
public class URLRestController {
    @Autowired
    private ShortUrlService service;
    @PostMapping("/url")
    public Object shortUrl(@RequestBody ShortUrlDto  url){
        if (LinkValidator.getInstance().isLinkValid(url.getUrl())) {
            return service.createShortUrl(url);
        }else {
//
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }
    }

    @GetMapping("{key}")
    public RedirectView fullUrl(@PathVariable("key") String key){
        return service.getFullUrl(key);
    }
}

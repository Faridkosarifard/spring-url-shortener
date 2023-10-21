package com.fk.shortUrl.web.controller;

import com.fk.shortUrl.entity.ShortUrlEntity;
import com.fk.shortUrl.service.ShortUrlService;
import com.fk.shortUrl.web.dto.ShortUrlDto;
import org.springframework.beans.factory.annotation.Autowired;
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
        return service.createShortUrl(url);
    }

    @GetMapping("{key}")
    public RedirectView fullUrl(@PathVariable("key") String key){
        return service.getFullUrl(key);
    }
}

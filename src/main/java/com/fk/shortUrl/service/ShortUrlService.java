package com.fk.shortUrl.service;

import com.fk.shortUrl.domain.entity.ShortUrlEntity;
import com.fk.shortUrl.repository.ShortUrlRepository;
import com.fk.shortUrl.util.ShortUrlUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.RedirectView;

/**
 * ShortUrl
 * Created by Farid Kosarifard
 * Date: 10/21/2023
 * Time: 8:09 PM
 */
@Service
@RequiredArgsConstructor
public class ShortUrlService {

    private final ShortUrlRepository repository;
    private final ShortUrlUtil urlUtil;
    public ShortUrlEntity createShortUrl(ShortUrlEntity urlDto){
        String fullUrl= urlDto.getFullUrl();
        ShortUrlEntity existShortUrl = repository.findByFullUrl(fullUrl);
        if (existShortUrl != null){
            return ShortUrlEntity.builder().key(existShortUrl.getKey()).build();
        }else {
            String newKey = urlUtil.generateUniqueKey();
            ShortUrlEntity newEntity = ShortUrlEntity.builder()
                    .key(newKey)
                    .fullUrl(fullUrl)
                    .clickCount(0L)
                    .build();
            repository.save(newEntity);
            return ShortUrlEntity.builder().key(newKey).fullUrl(urlDto.getFullUrl()).build();
        }
    }

    public RedirectView getFullUrl(String key){
        ShortUrlEntity entity= repository.findByKey(key);
        entity.setClickCount(entity.getClickCount()+1);
        repository.save(entity);
        return new RedirectView(entity.getFullUrl());
    }
}

package com.fk.shortUrl.repository;

import com.fk.shortUrl.domain.entity.ShortUrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ShortUrl
 * Created by Farid Kosarifard
 * Date: 10/21/2023
 * Time: 7:58 PM
 */

public interface ShortUrlRepository extends JpaRepository<ShortUrlEntity,Long> {
    ShortUrlEntity findByKey(String key);

    ShortUrlEntity findByFullUrl(String fullUrl);
}

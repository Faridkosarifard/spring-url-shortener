package com.fk.shortUrl.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * ShortUrl
 * Created by Farid Kosarifard
 * Date: 10/21/2023
 * Time: 8:01 PM
 */
@Getter
@Setter
@Builder
public class ShortUrlDto {
    private String url;
    private String key;
}

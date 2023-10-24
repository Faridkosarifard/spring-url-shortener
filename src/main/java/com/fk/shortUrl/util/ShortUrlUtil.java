package com.fk.shortUrl.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * ShortUrl
 * Created by Farid Kosarifard
 * Date: 10/21/2023
 * Time: 8:02 PM
 */
@Component
public class ShortUrlUtil {
    private static final Random RANDOM = new Random();

    @Value("${url.allowed-characters}")
    private String allowedCharacters;
    @Value("${url.key-length}")
    private int keyLength;


    public String generateUniqueKey(){

        StringBuilder keyBuilder = new StringBuilder();
        for (int i =0 ; i < keyLength; i++){
            int randomIndex = RANDOM.nextInt(allowedCharacters.length());
            keyBuilder.append(allowedCharacters.charAt(randomIndex));
        }
        return keyBuilder.toString();
    }
}

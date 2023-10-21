package com.fk.shortUrl.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ShortUrl
 * Created by Farid Kosarifard
 * Date: 10/21/2023
 * Time: 7:53 PM
 */
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "URLS")
public class ShortUrlEntity {
    @Id@GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(unique = true)
    private String key;

    @Column(nullable = false)
    private String fullUrl;

    private long clickCount;
}

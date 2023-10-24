package com.fk.shortUrl.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fk.shortUrl.domain.view.Views;
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
@Table(name = "URLS")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShortUrlEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "URLS_ID")
    @JsonIgnore
    private long id;

    @Column(name = "FULL_URL",nullable = false)
    @JsonView(Views.Public.class)
    private String fullUrl;

    @Column(name = "KEY", unique = true)
    @JsonView(Views.User.class)
    private String key;

    @Column(name = "CLICK_COUNT")
    @JsonIgnore
    private long clickCount;
}

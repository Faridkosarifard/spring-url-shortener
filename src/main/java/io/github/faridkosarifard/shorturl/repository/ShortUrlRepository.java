package io.github.faridkosarifard.shorturl.repository;

import io.github.faridkosarifard.shorturl.domain.ShortUrl;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByCode(String code);

    boolean existsByCode(String code);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ShortUrl s set s.clickCount = s.clickCount + 1 where s.code = :code")
    int incrementClickCount(@Param("code") String code);
}

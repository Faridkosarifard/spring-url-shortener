package io.github.faridkosarifard.shorturl.support;

import io.github.faridkosarifard.shorturl.config.ShortUrlProperties;
import io.github.faridkosarifard.shorturl.web.error.InvalidUrlException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class UrlValidator {

    public static final int MAX_URL_LENGTH = 2048;

    private final ShortUrlProperties properties;

    public UrlValidator(ShortUrlProperties properties) {
        this.properties = properties;
    }

    public String validate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidUrlException("URL must not be blank");
        }
        String value = raw.strip();
        if (value.length() > MAX_URL_LENGTH) {
            throw new InvalidUrlException("URL must be at most %d characters".formatted(MAX_URL_LENGTH));
        }

        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new InvalidUrlException("URL is not a valid URI");
        }

        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new InvalidUrlException("URL must be an absolute http or https URL");
        }

        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new InvalidUrlException("Only http and https URLs are allowed");
        }

        if (uri.getRawUserInfo() != null) {
            throw new InvalidUrlException("URLs with user information are not allowed");
        }

        String host = stripTrailingDot(uri.getHost().toLowerCase(Locale.ROOT));
        if (properties.isBlockedHost(host)) {
            throw new InvalidUrlException("This host is not allowed");
        }
        if (properties.blockPrivateHosts() && isPrivateOrLocalHost(host)) {
            throw new InvalidUrlException("Private or local hosts are not allowed");
        }
        return value;
    }

    static boolean isPrivateOrLocalHost(String host) {
        String normalized = stripTrailingDot(host.toLowerCase(Locale.ROOT));
        if ("localhost".equals(normalized)
                || "localhost.localdomain".equals(normalized)
                || normalized.endsWith(".localhost")) {
            return true;
        }
        InetAddress address = literalAddress(normalized);
        if (address == null) {
            return false;
        }
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isMulticastAddress()
                || isUniqueLocalIpv6(address)
                || isCarrierGradeNat(address);
    }

    private static InetAddress literalAddress(String host) {
        if (!isLiteralIp(host)) {
            return null;
        }
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException ex) {
            return null;
        }
    }

    private static boolean isLiteralIp(String host) {
        if (host.indexOf(':') >= 0) {
            return true;
        }
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) {
                return false;
            }
            for (int i = 0; i < part.length(); i++) {
                if (!Character.isDigit(part.charAt(i))) {
                    return false;
                }
            }
            int value = Integer.parseInt(part);
            if (value < 0 || value > 255) {
                return false;
            }
        }
        return true;
    }

    private static boolean isUniqueLocalIpv6(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length != 16) {
            return false;
        }
        return (bytes[0] & 0xFE) == 0xFC;
    }

    private static boolean isCarrierGradeNat(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length != 4) {
            return false;
        }
        int first = bytes[0] & 0xFF;
        int second = bytes[1] & 0xFF;
        return first == 100 && second >= 64 && second <= 127;
    }

    private static String stripTrailingDot(String host) {
        if (host.endsWith(".")) {
            return host.substring(0, host.length() - 1);
        }
        return host;
    }
}

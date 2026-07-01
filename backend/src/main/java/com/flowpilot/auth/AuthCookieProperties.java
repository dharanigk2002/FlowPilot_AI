package com.flowpilot.auth;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "app.security.auth-cookie")
public record AuthCookieProperties(
        String name,
        boolean secure,
        String sameSite
) {

    private static final Set<String> SUPPORTED_SAME_SITE_VALUES = Set.of("Lax", "Strict", "None");

    public AuthCookieProperties {
        Assert.hasText(name, "Authentication cookie name must not be blank.");
        Assert.isTrue(
                SUPPORTED_SAME_SITE_VALUES.contains(sameSite),
                "Authentication cookie SameSite must be Lax, Strict, or None."
        );
        Assert.isTrue(
                !"None".equals(sameSite) || secure,
                "Authentication cookies using SameSite=None must also be Secure."
        );
    }
}

package com.klaus.moply.auth.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Validated
@ConfigurationProperties("moply.auth.rate-limit")
public record AuthRateLimitProperties(@Min(1) @Max(10000) int loginIpBurst,
		@Min(1) @Max(86400) int loginIpRefillSeconds, @Min(1) @Max(10000) int loginAccountBurst,
		@Min(1) @Max(86400) int loginAccountRefillSeconds, @Min(1) @Max(10000) int signupIpBurst,
		@Min(1) @Max(86400) int signupIpRefillSeconds, @Min(3) @Max(1000000) int maxEntries,
		java.util.List<String> trustedProxies) {
}

package com.aisandbox.account.config;

import com.aisandbox.account.ratelimit.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the rate-limiting interceptor for all MVC endpoints. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final RateLimitInterceptor rateLimitInterceptor;

	public WebConfig(RateLimitInterceptor rateLimitInterceptor) {
		this.rateLimitInterceptor = rateLimitInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(rateLimitInterceptor);
	}

}

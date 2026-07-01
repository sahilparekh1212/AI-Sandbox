package com.aisandbox.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// Active in every profile except LOADTEST, which swaps in an auth-free chain
// (LoadTestSecurityConfig) so the CI load generator can exercise the real endpoints.
@Configuration
@EnableWebSecurity
@Profile("!LOADTEST")
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/**").permitAll()
				.anyRequest().authenticated()
			)
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
		return http.build();
	}

}

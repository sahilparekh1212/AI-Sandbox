package com.aisandbox.auth.controller;

import com.aisandbox.auth.model.RefreshRequest;
import com.aisandbox.auth.model.TokenResponse;
import com.aisandbox.auth.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Token management endpoints")
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final TokenService tokenService;

	public AuthController(TokenService tokenService) {
		this.tokenService = tokenService;
	}

	@PostMapping("/refresh")
	@Operation(summary = "Exchange a refresh token for a new access token (30 min TTL)")
	public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest request) {
		return tokenService.consumeRefreshToken(request.refreshToken())
			.map(userId -> {
				TokenResponse tokens = tokenService.generateTokens(userId, null, null);
				log.info("Refreshed access token for userId={}", userId);
				return ResponseEntity.ok(tokens);
			})
			.orElseGet(() -> {
				log.warn("Refresh attempt with invalid or expired token");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			});
	}

	@GetMapping("/me")
	@Operation(summary = "Return the authenticated user's claims from the JWT")
	public ResponseEntity<Map<String, Object>> me(JwtAuthenticationToken principal) {
		Map<String, Object> claims = principal.getToken().getClaims();
		return ResponseEntity.ok(Map.of(
			"userId", claims.getOrDefault("sub", ""),
			"email", claims.getOrDefault("email", ""),
			"name", claims.getOrDefault("name", "")
		));
	}

	@PostMapping("/logout")
	@Operation(summary = "Revoke a refresh token")
	public ResponseEntity<Void> logout(@RequestBody RefreshRequest request) {
		tokenService.revokeRefreshToken(request.refreshToken());
		return ResponseEntity.noContent().build();
	}

}

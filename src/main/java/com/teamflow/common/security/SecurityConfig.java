package com.teamflow.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth ->
                oauth
                    .jwt(Customizer.withDefaults())
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            writeError(response, mapper, ErrorCode.UNAUTHORIZED))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            writeError(response, mapper, ErrorCode.FORBIDDEN)))
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, exception) ->
                            writeError(response, mapper, ErrorCode.UNAUTHORIZED))
                    .accessDeniedHandler(
                        (request, response, exception) ->
                            writeError(response, mapper, ErrorCode.FORBIDDEN)))
        .build();
  }

  private void writeError(HttpServletResponse response, ObjectMapper mapper, ErrorCode error)
      throws IOException {
    response.setStatus(error.status().value());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    mapper.writeValue(
        response.getOutputStream(), ApiResponse.failure(error.code(), error.message()));
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecretKey jwtSecretKey(@Value("${teamflow.jwt.secret}") String secret) {
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) {
      throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
    }
    return new SecretKeySpec(bytes, "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
  }

  @Bean
  JwtDecoder jwtDecoder(SecretKey jwtSecretKey, @Value("${teamflow.jwt.issuer}") String issuer) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    return decoder;
  }
}

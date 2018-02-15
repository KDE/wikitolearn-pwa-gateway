package org.wikitolearn.gateway.pwa.config;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfiguration extends ResourceServerConfigurerAdapter {

  private final ResourceServerProperties resourceServerProperties;

  public SecurityConfiguration(ResourceServerProperties resourceServerProperties) {
    this.resourceServerProperties = resourceServerProperties;
  }

  @Override
  public void configure(HttpSecurity http) throws Exception {
    http
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    .and()
      .authorizeRequests()
      .antMatchers(HttpMethod.HEAD).permitAll()
      .antMatchers(HttpMethod.OPTIONS).permitAll()
      .antMatchers(HttpMethod.GET, "/api/**").permitAll()
      .antMatchers("/api/**").authenticated()
    .and()
      .cors()
    .and()
      .csrf()
      .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
  }
  
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public CorsConfigurationSource corsConfigurationSource() {
      final CorsConfiguration configuration = new CorsConfiguration();
      configuration.setAllowedOrigins(Arrays.asList("*"));
      configuration.setAllowedMethods(Arrays.asList("*"));
      configuration.setAllowedHeaders(Arrays.asList("*"));
      final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
      source.registerCorsConfiguration("/**", configuration);
      return source;
  }

  @Override
  public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
    resources.resourceId(resourceServerProperties.getResourceId());
  }

  @Bean
  @ConditionalOnProperty("security.oauth2.resource.jwt.key-uri")
  public TokenStore tokenStore(JwtAccessTokenConverter jwtAccessTokenConverter) {
    return new JwtTokenStore(jwtAccessTokenConverter);
  }

  @Bean
  @ConditionalOnProperty("security.oauth2.resource.jwt.key-uri")
  public JwtAccessTokenConverter jwtAccessTokenConverter() {
    JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
    converter.setVerifierKey(getKeyFromAuthorizationServer());
    return converter;
  }

  private String getKeyFromAuthorizationServer() {
    return Optional.ofNullable(
      new RestTemplate()
        .exchange(
          resourceServerProperties.getJwt().getKeyUri(),
          HttpMethod.GET,
          new HttpEntity<Void>(new HttpHeaders()),
          Map.class
        )
        .getBody()
        .get("public_key"))
    .map(publicKey -> String.format("-----BEGIN PUBLIC KEY-----\n%s\n-----END PUBLIC KEY-----", publicKey))
    .orElse(resourceServerProperties.getJwt().getKeyValue());
  }
}

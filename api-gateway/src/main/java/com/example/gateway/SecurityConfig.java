package com.example.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configuration
public class SecurityConfig {

    @Value("${API_USERNAME:admin}")
    private String username;

    @Value("${API_PASSWORD:admin}")
    private String password;

    @Value("${OIDC_ISSUER_URI:}")
    private String issuer;

    
    @Bean
    @ConditionalOnProperty(name = "OIDC_ISSUER_URI", havingValue = "", matchIfMissing = true)
    public UserDetailsService inMemoryUserDetailsService() {
        UserDetails user = User.withUsername(username)
                .password("{noop}" + password)
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable();

        http.authorizeHttpRequests(authz -> authz
            .requestMatchers("/swagger-ui.html", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated()
        );

        if (!issuer.isEmpty()) {
            // настраиваем OIDC JWT Resource Server с явным JwtDecoder
            http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            );
        } else {
            http.httpBasic();
        }

        return http.build();
    }

    // Создаём JwtDecoder вручную, вместо вызова issuerUri()
    @Bean
    @ConditionalOnProperty(name = "OIDC_ISSUER_URI")
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromOidcIssuerLocation(issuer);
    }
}

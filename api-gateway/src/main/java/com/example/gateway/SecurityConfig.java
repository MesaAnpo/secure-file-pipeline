package com.example.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${API_USERNAME:admin}")
    private String username;

    @Value("${API_PASSWORD:admin}")
    private String password;

    @Value("${OIDC_ISSUER_URI:}")
    private String issuer;

    @Bean
    @ConditionalOnProperty(name = "OIDC_ISSUER_URI", havingValue = "", matchIfMissing = true)
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails user = User.withUsername(username)
                .password("{noop}" + password)
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.authorizeHttpRequests((authz) -> authz
                .requestMatchers("/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated());

        if (!issuer.isEmpty()) {
            http.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.issuerUri(issuer)));
        } else {
            http.httpBasic();
        }
        return http.build();
    }
}

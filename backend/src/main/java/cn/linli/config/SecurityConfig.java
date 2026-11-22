package cn.linli.config;

import cn.linli.auth.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.time.Clock;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    Clock clock() { return Clock.systemUTC(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthService service,
                                            ObjectMapper json, Clock clock) throws Exception {
        // Only explicit Authorization headers authenticate requests; no cookies or HTTP sessions.
        return http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, ex) -> SecurityErrors.write(json, res, 401, "UNAUTHENTICATED", "请先登录"))
                .accessDeniedHandler((req, res, ex) -> SecurityErrors.write(json, res, 403, "FORBIDDEN", "无权访问该资源")))
            .addFilterBefore(new AuthRateLimitFilter(json, clock), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new BearerTokenFilter(service, json), UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}

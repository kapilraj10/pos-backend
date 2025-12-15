package kapil.raj.pos.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import kapil.raj.pos.filter.JwtRequestFilter;
import kapil.raj.pos.service.impl.AppUserDetailService;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppUserDetailService appUserDetailService;
    private final JwtRequestFilter jwtRequestFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                /** Enable CORS */
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)

                /** Authorization rules */
                .authorizeHttpRequests(auth -> auth

                        /** Allow CORS preflight */
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        /** Payment endpoints - allow guest checkout - MUST BE FIRST */
                        .requestMatchers("/payments/test").authenticated()
                        .requestMatchers(HttpMethod.POST, "/payments/initiate", "/payments/lookup").permitAll()

                        /** Public endpoints - Note: paths are relative to context-path */
                        .requestMatchers(HttpMethod.POST, "/login", "/encode").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories", "/items").permitAll()

                        /** Dashboard - accessible to authenticated users */
                        .requestMatchers(HttpMethod.GET, "/dashboard").authenticated()

                        /** Admin-only endpoints */
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/categories",
                                "/items",
                                "/orders"
                        ).hasAuthority("ROLE_ADMIN")

                        /** All other requests */
                        .anyRequest().authenticated()
                )

                /** Stateless JWT session */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        /** JWT filter */
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(appUserDetailService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authProvider);
    }

    /** CORS configuration */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "https://pos-frontend-liart-seven.vercel.app/",
                "https://pos-frontend-liart-seven.vercel.app/"
        ));

        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}

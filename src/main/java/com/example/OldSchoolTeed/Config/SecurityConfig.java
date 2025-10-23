package com.example.OldSchoolTeed.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Para permitir @PreAuthorize en los controladores
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, AuthenticationProvider authenticationProvider) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Deshabilitamos CSRF (común en APIs REST stateless)
                .csrf(csrf -> csrf.disable())

                // Configuración de CORS (Cross-Origin Resource Sharing)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Definición de rutas públicas y privadas
                .authorizeHttpRequests(auth -> auth
                        // --- RUTAS PÚBLICAS ---
                        // Los endpoints de autenticación (registro y login)
                        // (Recuerda que el context-path es /api/v1)
                        .requestMatchers("/auth/**").permitAll()

                        // Endpoints públicos del catálogo (ver productos, categorías)
                        .requestMatchers(HttpMethod.GET, "/productos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categorias/**").permitAll()

                        // (Opcional) Documentación de API (si usas Swagger/OpenAPI)
                        // .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()

                        // --- RUTAS PRIVADAS ---
                        // Rutas de administrador (ej. crear producto)
                        .requestMatchers("/admin/**").hasAuthority("Administrador")

                        // Rutas de cliente (ej. ver mi carrito, hacer pedido)
                        .requestMatchers("/carrito/**").hasAnyAuthority("Cliente", "Administrador")
                        .requestMatchers("/pedidos/**").hasAnyAuthority("Cliente", "Administrador")

                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )

                // Configuración de la sesión: STATELess (sin estado)
                // Spring Security no creará ni usará sesiones HTTP.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Añadimos nuestro proveedor de autenticación
                .authenticationProvider(authenticationProvider)

                // Añadimos nuestro filtro JWT antes del filtro de usuario/password
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Bean para la configuración de CORS
    // Esto permite que tu frontend de Angular (que correrá en otro puerto)
    // pueda comunicarse con tu API.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permite peticiones desde el origen de Angular (ej. http://localhost:4200)
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplicamos esta configuración a todas las rutas
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

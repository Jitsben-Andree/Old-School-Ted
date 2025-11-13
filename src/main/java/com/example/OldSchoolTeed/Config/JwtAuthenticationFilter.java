package com.example.OldSchoolTeed.Config;

import com.example.OldSchoolTeed.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger; // Importar Logger
import org.slf4j.LoggerFactory; // Importar LoggerFactory
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class); // Añadir logger
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Log para ver la petición entrante y la cabecera
        log.trace("Procesando petición: {} {}", request.getMethod(), request.getRequestURI());
        log.trace("Cabecera Authorization: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.trace("No se encontró cabecera Bearer, continuando cadena de filtros sin autenticación JWT.");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        log.trace("Token JWT extraído: {}", jwt);

        try {
            userEmail = jwtService.extractUsername(jwt);
            log.debug("Email extraído del token: {}", userEmail);
        } catch (Exception e) {
            log.warn("Token JWT inválido o expirado: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token JWT inválido o expirado");
            return; // Detener la cadena si el token es inválido
        }

        // Si tenemos email y el usuario AÚN NO está autenticado en esta petición
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("Usuario {} no autenticado en SecurityContext, cargando UserDetails...", userEmail);
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // *** LOG CLAVE: Imprimir las autoridades (roles) cargadas ***
            log.info("UserDetails cargados para {}. Autoridades: {}", userEmail, userDetails.getAuthorities());


            if (jwtService.isTokenValid(jwt, userDetails)) {
                log.debug("Token JWT válido para {}. Autenticando...", userEmail);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities() //
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                // Establecer la autenticación en el contexto
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("Usuario {} autenticado exitosamente en SecurityContext.", userEmail);
            } else {
                log.warn("Token JWT NO válido para usuario {}", userEmail);
            }
        } else if (userEmail != null) {
            log.trace("Usuario {} ya estaba autenticado en SecurityContext.", userEmail);
        }

        // Continuar con el siguiente filtro
        filterChain.doFilter(request, response);
    }
}

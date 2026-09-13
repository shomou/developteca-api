package com.developteca.config;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.developteca.service.JwtService;
import com.developteca.service.UserDetailsServiceImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService){
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException{
        
        try{
            // 1. Extraer JWT del header Authorization
            String jwt = extractJwtFromRequest(request);
            
            // 2. Validar que el token existe
            if(jwt != null && jwtService.isTokenValid(jwt) ){

                // 3. Extraer username (email) del token
                String email = jwtService.extractUsername(jwt);
                
                // 4. Cargar detalles del usuario
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 5. Crear token de autenticación
                UsernamePasswordAuthenticationToken authenticationToken = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                // 6. Establecer detalles de la solicitud
                authenticationToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 7. Establecer la autenticación en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (Exception ex) {
            logger.error("No se pudo establecer autenticación de usuario: " + ex.getMessage(), ex);
        }

        // 8. Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }

    // ============= EXTRAER JWT DEL HEADER =============
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // Eliminar "Bearer "
        }
        return null;
    }
}
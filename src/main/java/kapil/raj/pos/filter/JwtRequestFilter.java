package kapil.raj.pos.filter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kapil.raj.pos.service.impl.AppUserDetailService;
import kapil.raj.pos.util.JwtUtil;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);
    
    private final AppUserDetailService userDetailService;
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        // Log all incoming requests for debugging
        logger.info("📥 Incoming request: {} {}", method, uri);
        
        // Skip JWT processing for public payment endpoints (guest checkout)
        if (uri.contains("/payments/initiate") || uri.contains("/payments/lookup")) {
            logger.info("🔓 Public payment endpoint - skipping JWT authentication: {} {}", method, uri);
            filterChain.doFilter(request, response);
            return;
        }
        
        // Skip JWT processing for public endpoints
        if (uri.contains("/login") || uri.contains("/encode") || 
            (method.equals("GET") && (uri.contains("/categories") || uri.contains("/items")))) {
            logger.info("🔓 Public endpoint - skipping JWT authentication: {} {}", method, uri);
            filterChain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");
        String email = null;
        String jwt = null;
        
        // DEBUG: Log admin requests
        if ("POST".equals(method) || "DELETE".equals(method)) {
            logger.warn("🔍 {} {} | Auth Header: {} | Content-Type: {}", 
                method,
                uri,
                authorizationHeader != null ? "Present " : "MISSING ",
                request.getContentType());
        }

        if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer ")) {
            // Support case-insensitive Bearer prefix and trim whitespace
            jwt = authorizationHeader.substring(authorizationHeader.indexOf(' ') + 1).trim();
            try {
                email = jwtUtil.extractUsername(jwt);
                logger.info(" Token valid for user: {}", email);
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                logger.warn(" Token EXPIRED for: {} | Expired at: {} | Endpoint: {}", 
                    e.getClaims().getSubject(), 
                    e.getClaims().getExpiration(),
                    uri);
                
                
                if (uri.matches(".*/items$|.*/categories$")) {
                    logger.info(" Allowing public endpoint access despite expired token: {}", uri);
                } else {
                  
                    response.setHeader("X-Token-Expired", "true");
                }
                jwt = null;
                email = null;
            } catch (Exception e) {
                logger.error(" Token extraction failed: {}", e.getMessage());
                jwt = null;
                email = null;
            }
        } else {
            if (authorizationHeader != null) {
                logger.warn(" Authorization header present but not using Bearer scheme: {}", authorizationHeader);
            } else if (uri.contains("/admin/")) {
                logger.error(" Admin endpoint accessed without Authorization header: {}", uri);
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailService.loadUserByUsername(email);

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    List<String> roles = jwtUtil.extractRoles(jwt);
                    List<SimpleGrantedAuthority> authorities;
                    
                    if (roles != null && !roles.isEmpty()) {
                        authorities = roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());
                        logger.info(" Authorities from JWT: {}", authorities);
                    } else {
                        authorities = userDetails.getAuthorities().stream()
                                .map(auth -> new SimpleGrantedAuthority(auth.getAuthority()))
                                .collect(Collectors.toList());
                        logger.info(" Authorities from DB: {}", authorities);
                    }

                    // extra defensive check
                    if (authorities.isEmpty()) {
                        logger.warn(" No authorities found for user {} - authentication may fail", email);
                    }
                    
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info(" Authentication set successfully for: {}", email);
                } else {
                    logger.error(" Token validation failed for: {} - token may be invalid or secret key mismatch", email);
                }
            } catch (Exception e) {
                logger.error(" Authentication failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}

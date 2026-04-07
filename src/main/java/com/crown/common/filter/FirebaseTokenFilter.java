package com.crown.common.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private final JdbcTemplate jdbcTemplate;

    public FirebaseTokenFilter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String idToken = header.substring(7);
            try {
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                decodedToken,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // DAU 추적: Firebase UID → member_id 조회 후 request attribute 설정
                String uid = decodedToken.getUid();
                List<Long> ids = jdbcTemplate.queryForList(
                        "SELECT member_id FROM member WHERE google_id = ? LIMIT 1", Long.class, uid);
                if (!ids.isEmpty()) {
                    AccessLogInterceptor.setMemberId(request, ids.get(0));
                }
            } catch (Exception e) {
                logger.warn("Firebase token verification failed: " + e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }
}

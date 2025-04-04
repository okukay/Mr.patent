package com.d208.mr_patent_backend.global.jwt;

import com.d208.mr_patent_backend.global.jwt.exception.JwtErrorCode;
import com.d208.mr_patent_backend.global.jwt.exception.JwtException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();

        // OPTIONS 요청은 바로 통과 (CORS preflight 요청)
        if (httpRequest.getMethod().equals("OPTIONS")) {
            chain.doFilter(request, response);
            return;
        }

        String token = resolveToken(httpRequest);

        try {
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (isAuthenticationRequired(requestURI)) {
                throw new JwtException(JwtErrorCode.TOKEN_NOT_FOUND);
            }
        } catch (JwtException e) {
            if (isAuthenticationRequired(requestURI)) {
                sendErrorResponse((HttpServletResponse) response, e.getErrorCode(), e.getMessage());
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isAuthenticationRequired(String requestURI) {
        return !(requestURI.startsWith("/api/swagger-ui") ||
                requestURI.startsWith("/api/v3/api-docs") ||
                requestURI.equals("/api/user") ||
                requestURI.equals("/api/user/expert") ||
                requestURI.equals("/api/user/login") ||
                requestURI.equals("/profile-upload-test.html") ||
                requestURI.equals("/api/expert-approve") ||
                requestURI.startsWith("/api/user/profile-image") ||
                requestURI.startsWith("/api/email/")||
                requestURI.startsWith("/ws/chat")
        );
    }

    private void sendErrorResponse(HttpServletResponse response, JwtErrorCode errorCode, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("code", errorCode.name());
        errorResponse.put("message", message);

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
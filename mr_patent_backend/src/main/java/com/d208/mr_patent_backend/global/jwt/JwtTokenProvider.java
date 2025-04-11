package com.d208.mr_patent_backend.global.jwt;

import com.d208.mr_patent_backend.global.jwt.exception.JwtErrorCode;
import com.d208.mr_patent_backend.global.jwt.exception.JwtException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenValidityInMilliseconds;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenValidityInMilliseconds;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateAccessToken(Authentication authentication,Integer userId) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date accessTokenValidity = new Date(now + accessTokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(authentication.getName())
                .claim("auth", authorities)
                .claim("userId", userId)  // userId 추가
                .setExpiration(accessTokenValidity)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken() {
        long now = (new Date()).getTime();
        Date refreshTokenValidity = new Date(now + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .setExpiration(refreshTokenValidity)
                .signWith(key)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        if (claims.get("auth") == null) {
            throw new JwtException(JwtErrorCode.TOKEN_INVALID, "권한 정보가 없는 토큰입니다.");
        }

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new JwtException(JwtErrorCode.TOKEN_EXPIRED);
        } catch (MalformedJwtException e) {
            throw new JwtException(JwtErrorCode.TOKEN_MALFORMED);
        } catch (UnsupportedJwtException e) {
            throw new JwtException(JwtErrorCode.TOKEN_UNSUPPORTED);
        } catch (IllegalArgumentException e) {
            throw new JwtException(JwtErrorCode.TOKEN_ILLEGAL_ARGUMENT);
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public String getUserEmail(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    // userId 추출 메서드 추가
    public Integer getUserId(String token) {
        Claims claims = parseClaims(token);
        return claims.get("userId", Integer.class);
    }
}
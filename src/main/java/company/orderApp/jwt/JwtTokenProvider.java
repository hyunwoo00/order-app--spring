package company.orderApp.jwt;

import company.orderApp.domain.User;
import company.orderApp.repository.UserRepository;
import company.orderApp.service.exception.NonExistentUserException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    private final Key key;
    private final UserRepository userRepository;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey, UserRepository userRepository) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.userRepository = userRepository;
    }

    public JwtToken generateToken(Long id, Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();


        //Access Token 생성
        Date accessTokenExpire = new Date(now + 86400000 / 12); //2시간
        String accessToken = Jwts.builder()
                .setSubject(String.valueOf(id))
                .claim("auth", authorities)
                .setExpiration(accessTokenExpire)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        //Refresh Token 생성
        Date refreshTokenExpire = new Date(now + 86400000L * 30); //30일
        String refreshToken = Jwts.builder()
                .setExpiration(refreshTokenExpire)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return JwtToken.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpire(accessTokenExpire)
                .refreshTokenExpire(refreshTokenExpire)
                .build();
    }

    public String generateAccessToken(Long id, String authorities) {


        long now = (new Date()).getTime();

        //Access Token 생성
        Date accessTokenExpire = new Date(now + 86400000 / 12);
        return Jwts.builder()
                .setSubject(String.valueOf(id))
                .claim("auth", authorities)
                .setExpiration(accessTokenExpire)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Authentication getAuthentication(String accessToken) {
        //Jwt 토큰 복호화
        Claims claims = parseClaims(accessToken);

        if (claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }
        String strId = getUserId(accessToken);
        Long id = Long.parseLong(strId);

        User user =userRepository.findById(id)
                .orElseThrow(() -> new NonExistentUserException("존재하지 않는 회원입니다."));

        //클레임에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> auths = user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(user, "", auths);
    }

    public Long getExpiration(String accessToken) {
        //Jwt 토큰 복호화
        Claims claims = parseClaims(accessToken);

        return claims.getExpiration().getTime();
    }

    public String getUserId(String accessToken) {
        //Jwt 토큰 복호화
        Claims claims = parseClaims(accessToken);

        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try{
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        }catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        }
        return false;
    }

    public Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    //JWS(Json Web Signature) 서버에서 인증을 근거로 인증정보를 서버의 private key로 서명한 것을 토큰화 한 것.
                    .parseClaimsJws(accessToken)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}

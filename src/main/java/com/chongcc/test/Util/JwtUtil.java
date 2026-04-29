package com.chongcc.test.Util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

public class JwtUtil {
    private static final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_TIME = 1000 * 60 * 60;

    public static String generateToken(String userName, String permission){
        return Jwts.builder()
                .setSubject(userName)
                .claim("permission", permission)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static String getUserNameFromToken(String token){
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public static String getPermissionFromToken(String token){
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("permission", String.class);
    }

    public static boolean validateToken(String token) {
        try {
            // 确保使用 parseClaimsJws() 而非 parse()
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);  // 注意是 parseClaimsJws()！
            return true;
        } catch (ExpiredJwtException e) {
            System.err.println("Token 已过期");
        } catch (UnsupportedJwtException e) {
            System.err.println("Token 结构不支持"+e);  // 这里会捕获您的错误
        } catch (MalformedJwtException e) {
            System.err.println("Token 格式错误");
        }
        return false;
    }
}

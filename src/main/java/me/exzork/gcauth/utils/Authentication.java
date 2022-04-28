package me.exzork.gcauth.utils;

import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import me.exzork.gcauth.GCAuth;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.util.HashMap;

public final class Authentication {
    public static final HashMap<String,String> tokens = new HashMap<String,String>();
    private static SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    public static SecretKey getKey() {
        return key;
    }

    public static Account getAccountByUsernameAndPassword(String username, String password) {
        Account account = DatabaseHelper.getAccountByName(username);
        if(account.getPassword() != null && !account.getPassword().isEmpty()) {
            if(!verifyPassword(password, account.getPassword())) account = null;
        }
        return account;
    }

    public static String generateOneTimeToken(Account account) {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 32; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        Authentication.tokens.put(sb.toString(), account.getUsername());
        return sb.toString();
    }

    public static String generateJwt(Account account) {
        String jws = Jwts.builder()
                .signWith(Authentication.getKey())
                .claim("token",generateOneTimeToken(account))
                .claim("username",account.getUsername())
                .claim("uid",account.getPlayerUid())
                .compact();
        return jws;
    }

    public static String generateHash(String password) {
        return switch (GCAuth.getConfig().Hash.toLowerCase()) {
            case "bcrypt" -> new BCryptPasswordEncoder().encode(password);
            case "scrypt" -> new SCryptPasswordEncoder().encode(password);
            default -> password;
        };
    }

    private static boolean verifyPassword(String password, String hash) {
        return switch (GCAuth.getConfig().Hash.toLowerCase()) {
            case "bcrypt" -> new BCryptPasswordEncoder().matches(password, hash);
            case "scrypt" -> new SCryptPasswordEncoder().matches(password, hash);
            default -> password.equals(hash);
        };
    }
}

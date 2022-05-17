package me.exzork.gcauth.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import me.exzork.gcauth.GCAuth;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;

import java.util.HashMap;

public final class Authentication {
    public static final HashMap<String,String> tokens = new HashMap<>();
    private static Algorithm key = Algorithm.HMAC256(generateRandomString(32));
    public static Algorithm getKey() {
        return key;
    }

    public static Account getAccountByUsernameAndPassword(String username, String password) {
        Account account = DatabaseHelper.getAccountByName(username);
        if (account == null) return null;
        if(account.getPassword() != null && !account.getPassword().isEmpty()) {
            if(!verifyPassword(password, account.getPassword())) account = null;
        }
        return account;
    }

    public static Account getAccountByOneTimeToken(String token) {
        String username = Authentication.tokens.get(token);
        if (username == null) return null;
        Authentication.tokens.remove(token);
        return DatabaseHelper.getAccountByName(username);
    }

    public static String generateRandomString(int length){
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < length; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    public static String generateOneTimeToken(Account account) {
        String token = Authentication.generateRandomString(32);
        Authentication.tokens.put(token, account.getUsername());
        return token;
    }

    public static String generateJwt(Account account) {
        return JWT.create()
                .withClaim("token",generateOneTimeToken(account))
                .withClaim("username",account.getUsername())
                .withClaim("uid",account.getPlayerUid())
                .sign(key);
    }

    public static String generateHash(String password) {
        return switch (GCAuth.getConfigStatic().Hash.toLowerCase()) {
            case "bcrypt" -> new BCryptPasswordEncoder().encode(password);
            case "scrypt" -> new SCryptPasswordEncoder().encode(password);
            default -> password;
        };
    }

    private static boolean verifyPassword(String password, String hash) {
        return switch (GCAuth.getConfigStatic().Hash.toLowerCase()) {
            case "bcrypt" -> new BCryptPasswordEncoder().matches(password, hash);
            case "scrypt" -> new SCryptPasswordEncoder().matches(password, hash);
            default -> password.equals(hash);
        };
    }
}

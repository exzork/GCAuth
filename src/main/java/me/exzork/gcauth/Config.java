package me.exzork.gcauth;

import me.exzork.gcauth.utils.Authentication;

public final class Config {
    public String hash = "BCRYPT";
    public String jwtSecret = Authentication.generateRandomString(32);
    public long jwtExpiration = 86400;
    public long otpExpiration = 300;
    public String[] defaultPermissions = new String[]{""};
    public String accessKey = "";
    public RateLimit rateLimit = new RateLimit();
    public static class RateLimit {
        public int maxRequests = 10;
        public String timeUnit = "MINUTES";
        public String[] endPoints = new String[]{"login","register","change_password"};
    }
}

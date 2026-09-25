package com.timetable.service;

import com.timetable.dao.AdminAccountDAO;
import com.timetable.model.AdminAccount;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** First-run administrator setup and PBKDF2 password verification. */
public class AdminAuthService {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private final AdminAccountDAO dao = new AdminAccountDAO();

    public boolean isConfigured() { return dao.exists(); }

    public void createInitialAdmin(String username, char[] password) {
        String cleanUsername = username == null ? "" : username.trim();
        if (cleanUsername.length() < 3 || cleanUsername.length() > 60)
            throw new IllegalArgumentException("Username must be between 3 and 60 characters.");
        if (password == null || password.length < 8)
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltText = Base64.getEncoder().encodeToString(salt);
        String hash = hash(password, salt);
        dao.create(new AdminAccount(cleanUsername, saltText, hash));
    }

    public boolean authenticate(String username, char[] password) {
        if (username == null || password == null) return false;
        AdminAccount account = dao.findByUsername(username);
        if (account == null) return false;
        byte[] salt = Base64.getDecoder().decode(account.getPasswordSalt());
        byte[] expected = Base64.getDecoder().decode(account.getPasswordHash());
        byte[] actual = Base64.getDecoder().decode(hash(password, salt));
        return MessageDigest.isEqual(expected, actual);
    }

    private String hash(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(key);
        } catch (Exception e) {
            throw new IllegalStateException("Password security service is unavailable.", e);
        } finally {
            spec.clearPassword();
        }
    }
}

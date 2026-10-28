package com.swornima.petcare.db;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Salted SHA-256 password hashing for locally stored accounts. Not as
 * strong as bcrypt/scrypt, but avoids pulling in a third-party crypto
 * dependency for this app's scope; passwords are never stored in plain text.
 */
public final class PasswordHasher {

    private static final int SALT_BYTES = 16;

    private PasswordHasher() {
    }

    public static String hash(String password) {
        try {
            byte[] salt = new byte[SALT_BYTES];
            new SecureRandom().nextBytes(salt);

            byte[] digest = digest(password, salt);

            return Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + Base64.encodeToString(digest, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean matches(String password, String stored) {
        if (stored == null || !stored.contains(":")) {
            return false;
        }
        try {
            String[] parts = stored.split(":", 2);
            byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] expected = Base64.decode(parts[1], Base64.NO_WRAP);
            byte[] actual = digest(password, salt);
            return Arrays.equals(expected, actual);
        } catch (NoSuchAlgorithmException | IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] digest(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        return md.digest(password.getBytes());
    }
}

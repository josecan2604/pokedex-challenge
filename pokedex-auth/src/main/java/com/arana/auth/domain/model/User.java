package com.arana.auth.domain.model;

import com.arana.auth.domain.exception.ValidationException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public record User(Long id, String username, String email, String passwordHash, Role role, Instant createdAt) {

    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_.-]{3,30}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 72;

    public static void validateRegistration(String username, String email, String rawPassword) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (username == null || !USERNAME.matcher(username).matches()) {
            errors.put("username", "must be 3-30 characters of letters, digits, '_', '.' or '-'");
        }
        if (email == null || email.length() > 254 || !EMAIL.matcher(email).matches()) {
            errors.put("email", "must be a valid email address");
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH || rawPassword.length() > MAX_PASSWORD_LENGTH) {
            errors.put("password", "must be between " + MIN_PASSWORD_LENGTH + " and " + MAX_PASSWORD_LENGTH + " characters");
        } else if (rawPassword.chars().noneMatch(Character::isLetter) || rawPassword.chars().noneMatch(Character::isDigit)) {
            errors.put("password", "must contain at least one letter and one digit");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public static String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}

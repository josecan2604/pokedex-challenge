package com.arana.auth.domain.model;

import java.time.Duration;

public record AccessToken(String value, Duration expiresIn) {
}

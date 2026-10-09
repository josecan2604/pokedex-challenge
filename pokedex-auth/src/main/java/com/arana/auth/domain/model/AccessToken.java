package com.arana.auth.domain.model;

import java.time.Duration;

/**
 * A signed access token and how long it is valid.
 */
public record AccessToken(String value, Duration expiresIn) {
}

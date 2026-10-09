package com.arana.auth.domain.model;

/**
 * USER can sync and edit local Pokemon; ADMIN can also delete them.
 * Sent to the resource servers as the {@code roles} claim.
 */
public enum Role {
    USER,
    ADMIN
}

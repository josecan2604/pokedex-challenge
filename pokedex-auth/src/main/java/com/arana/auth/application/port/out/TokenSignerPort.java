package com.arana.auth.application.port.out;

import com.arana.auth.domain.model.AccessToken;
import com.arana.auth.domain.model.User;

/**
 * Turns a verified user into a signed access token. The JWT format, claims and keys
 * are infrastructure details the application layer does not see.
 */
public interface TokenSignerPort {

    AccessToken sign(User user);
}

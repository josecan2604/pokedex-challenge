package com.arana.auth.application.port.in;

import com.arana.auth.domain.model.AccessToken;

public interface IssueTokenUseCase {

    /**
     * Resource Owner Password Credentials grant: verifies the credentials and signs a token.
     *
     * @throws com.arana.auth.domain.exception.InvalidCredentialsException on a wrong username or password
     */
    AccessToken issueForCredentials(String username, String rawPassword);
}

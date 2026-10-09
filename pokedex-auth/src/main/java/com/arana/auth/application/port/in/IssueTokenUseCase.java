package com.arana.auth.application.port.in;

import com.arana.auth.domain.model.AccessToken;

public interface IssueTokenUseCase {

    AccessToken issueForCredentials(String username, String rawPassword);
}

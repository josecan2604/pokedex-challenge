package com.arana.auth.application.port.out;

import com.arana.auth.domain.model.AccessToken;
import com.arana.auth.domain.model.User;

public interface TokenSignerPort {

    AccessToken sign(User user);
}

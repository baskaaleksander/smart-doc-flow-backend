package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.AuthUserCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.AuthUserQueryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class AuthUserJpaAdapter implements AuthUserQueryPort, AuthUserCommandPort {
}

package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.PasswordResetTokenCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.PasswordResetTokenQueryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class PasswordResetTokenJpaAdapter implements PasswordResetTokenCommandPort, PasswordResetTokenQueryPort {
}

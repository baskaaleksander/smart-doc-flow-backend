package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.RefreshTokenCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.RefreshTokenQueryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class RefreshTokenJpaAdapter implements RefreshTokenCommandPort, RefreshTokenQueryPort {
}

package com.baskaaleksander.smartdocflowbackend.modules.auth.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.RoleCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.auth.domain.port.RoleQueryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class RoleJpaAdapter implements RoleQueryPort, RoleCommandPort {
}

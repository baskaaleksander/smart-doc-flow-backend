package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.jpa;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentUserBasic;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentUserQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DocumentUserJpaAdapter implements DocumentUserQueryPort {

    private final SpringDataUserRepository userRepo;

    @Override
    public Optional<DocumentUserBasic> findByUsername(String username) {
        ;
        return userRepo.findByUsername(username).map((e) -> new DocumentUserBasic(
                e.getId(),
                e.getUsername()
        ));
    }
}

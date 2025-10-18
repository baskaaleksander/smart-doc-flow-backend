package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewerBasic;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.ExternalUserQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class ExternalUserJpaAdapter implements ExternalUserQueryPort {

    private final SpringDataUserRepository userRepo;

    public ExternalUserJpaAdapter(SpringDataUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public Optional<String> findByUsername(String username) {
        return userRepo.findByUsername(username).map(UserEntity::getUsername);
    }

    @Override
    public Optional<String> findById(UUID id) {
        return userRepo.findById(id).map(UserEntity::getUsername);
    }

    @Override
    public Optional<UUID> findIdByUsername(String username) {
        return userRepo.findByUsername(username).map(UserEntity::getId);
    }

    @Override
    public Optional<ReviewerBasic> findReviewerByUsername(String username) {
        return userRepo.findByUsername(username).map(u -> new ReviewerBasic(u.getId(), u.getUsername()));
    }

}

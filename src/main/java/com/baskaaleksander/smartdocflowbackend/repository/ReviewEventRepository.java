package com.baskaaleksander.smartdocflowbackend.repository;

import com.baskaaleksander.smartdocflowbackend.model.ReviewEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewEventRepository extends JpaRepository<ReviewEvent, UUID> {
}

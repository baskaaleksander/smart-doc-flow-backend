package com.baskaaleksander.smartdocflowbackend.common.security.access;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component("docAccess")
public class DocumentAccessEvaluation {

    private final SpringDataDocumentRepository documentRepository;

    @Autowired
    public DocumentAccessEvaluation(
            SpringDataDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public boolean canView(String docId, Authentication authentication) {

        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_REVIEW")) return true;

        UserDetails user = (UserDetails) authentication.getPrincipal();

        return documentRepository.getOwnerUsernameById(UUID.fromString(docId)).map(ownerId -> ownerId.equals(user.getUsername())).orElse(false);
    }

    public boolean canModify(String docId, Authentication authentication) {

        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        if (roles.contains("ROLE_ADMIN")) return true;

        UserDetails user = (UserDetails) authentication.getPrincipal();

        return documentRepository.getOwnerUsernameById(UUID.fromString(docId)).map(ownerId -> ownerId.equals(user.getUsername())).orElse(false);
    }

}

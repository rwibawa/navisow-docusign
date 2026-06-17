package com.navisow.docusign.domain.admin;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAdminStateRepository extends JpaRepository<UserAdminState, UUID> {

    Optional<UserAdminState> findByUserId(UUID userId);
}

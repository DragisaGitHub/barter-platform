package com.barterplatform.web.bootstrap;

import com.barterplatform.domain.identity.entity.RoleEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.entity.UserRoleEntity;
import com.barterplatform.domain.identity.entity.UserRoleId;
import com.barterplatform.domain.identity.enums.RoleCode;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.infrastructure.identity.repository.RoleRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitialAdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminBootstrap.class);

    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialAdminBootstrap(AdminBootstrapProperties properties,
                                 UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 UserRoleRepository userRoleRepository,
                                 PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        if (!properties.enabled()) {
            log.debug("Admin bootstrap is disabled");
            return;
        }

        validateProperties();

        if (adminAlreadyExists()) {
            log.info("Admin user already exists, skipping bootstrap");
            return;
        }

        UserEntity admin = createAdminUser();
        assignAdminRole(admin);

        log.info("Initial admin user '{}' created successfully", admin.getUsername());
    }

    private void validateProperties() {
        if (isBlank(properties.username())) {
            throw new IllegalStateException("barter.bootstrap.admin.username must be set when admin bootstrap is enabled");
        }
        if (isBlank(properties.email())) {
            throw new IllegalStateException("barter.bootstrap.admin.email must be set when admin bootstrap is enabled");
        }
        if (isBlank(properties.password())) {
            throw new IllegalStateException("barter.bootstrap.admin.password must be set when admin bootstrap is enabled");
        }
    }

    private boolean adminAlreadyExists() {
        return userRepository.existsByEmail(properties.email())
                || userRepository.existsByUsername(properties.username());
    }

    private UserEntity createAdminUser() {
        UserEntity user = new UserEntity();
        user.setUsername(properties.username());
        user.setEmail(properties.email());
        user.setPasswordHash(passwordEncoder.encode(properties.password()));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setMfaEnabled(false);
        return userRepository.save(user);
    }

    private void assignAdminRole(UserEntity admin) {
        RoleEntity adminRole = roleRepository.findByCode(RoleCode.ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "ADMIN role not found in database. Ensure seed data is loaded before bootstrap."));

        UserRoleId userRoleId = new UserRoleId();
        userRoleId.setUserId(admin.getId());
        userRoleId.setRoleId(adminRole.getId());

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setId(userRoleId);
        userRole.setAssignedAt(OffsetDateTime.now());

        userRoleRepository.save(userRole);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}


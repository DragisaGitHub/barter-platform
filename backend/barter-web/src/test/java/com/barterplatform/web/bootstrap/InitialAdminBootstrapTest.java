package com.barterplatform.web.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.barterplatform.domain.identity.entity.RoleEntity;
import com.barterplatform.domain.identity.entity.UserEntity;
import com.barterplatform.domain.identity.entity.UserRoleEntity;
import com.barterplatform.domain.identity.enums.RoleCode;
import com.barterplatform.domain.identity.enums.UserStatus;
import com.barterplatform.infrastructure.identity.repository.RoleRepository;
import com.barterplatform.infrastructure.identity.repository.UserRepository;
import com.barterplatform.infrastructure.identity.repository.UserRoleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class InitialAdminBootstrapTest {

    @Mock
    private AdminBootstrapProperties properties;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private InitialAdminBootstrap bootstrap;

    @Test
    void disabledBootstrap_doesNothing() {
        when(properties.enabled()).thenReturn(false);

        bootstrap.onApplicationReady();

        verifyNoInteractions(userRepository, roleRepository, userRoleRepository, passwordEncoder);
    }

    @Test
    void enabledBootstrap_createsAdminUser() {
        when(properties.enabled()).thenReturn(true);
        when(properties.username()).thenReturn("admin");
        when(properties.email()).thenReturn("admin@example.com");
        when(properties.password()).thenReturn("secret123");

        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-password");

        UserEntity savedUser = new UserEntity();
        savedUser.setId(1L);
        savedUser.setUsername("admin");
        savedUser.setEmail("admin@example.com");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        RoleEntity adminRole = new RoleEntity();
        adminRole.setId(10L);
        adminRole.setCode(RoleCode.ADMIN);
        when(roleRepository.findByCode(RoleCode.ADMIN)).thenReturn(Optional.of(adminRole));

        when(userRoleRepository.save(any(UserRoleEntity.class))).thenAnswer(i -> i.getArgument(0));

        bootstrap.onApplicationReady();

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        UserEntity captured = userCaptor.getValue();
        assertEquals("admin", captured.getUsername());
        assertEquals("admin@example.com", captured.getEmail());
        assertEquals("hashed-password", captured.getPasswordHash());
        assertEquals(UserStatus.ACTIVE, captured.getStatus());
        assertTrue(captured.isEmailVerified());

        verify(userRoleRepository).save(any(UserRoleEntity.class));
    }

    @Test
    void existingAdminByEmail_isNotDuplicated() {
        when(properties.enabled()).thenReturn(true);
        when(properties.username()).thenReturn("admin");
        when(properties.email()).thenReturn("admin@example.com");
        when(properties.password()).thenReturn("secret123");

        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        bootstrap.onApplicationReady();

        verify(userRepository, never()).save(any(UserEntity.class));
        verifyNoInteractions(userRoleRepository);
    }

    @Test
    void existingAdminByUsername_isNotDuplicated() {
        when(properties.enabled()).thenReturn(true);
        when(properties.username()).thenReturn("admin");
        when(properties.email()).thenReturn("admin@example.com");
        when(properties.password()).thenReturn("secret123");

        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        bootstrap.onApplicationReady();

        verify(userRepository, never()).save(any(UserEntity.class));
        verifyNoInteractions(userRoleRepository);
    }

    @Test
    void adminRoleAssignment_isCreated() {
        when(properties.enabled()).thenReturn(true);
        when(properties.username()).thenReturn("admin");
        when(properties.email()).thenReturn("admin@example.com");
        when(properties.password()).thenReturn("secret123");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        UserEntity savedUser = new UserEntity();
        savedUser.setId(5L);
        savedUser.setUsername("admin");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        RoleEntity adminRole = new RoleEntity();
        adminRole.setId(10L);
        adminRole.setCode(RoleCode.ADMIN);
        when(roleRepository.findByCode(RoleCode.ADMIN)).thenReturn(Optional.of(adminRole));

        when(userRoleRepository.save(any(UserRoleEntity.class))).thenAnswer(i -> i.getArgument(0));

        bootstrap.onApplicationReady();

        ArgumentCaptor<UserRoleEntity> captor = ArgumentCaptor.forClass(UserRoleEntity.class);
        verify(userRoleRepository).save(captor.capture());

        UserRoleEntity userRole = captor.getValue();
        assertEquals(5L, userRole.getId().getUserId());
        assertEquals(10L, userRole.getId().getRoleId());
    }

    @Test
    void enabledWithMissingUsername_throwsException() {
        when(properties.enabled()).thenReturn(true);
        when(properties.username()).thenReturn("");

        assertThrows(IllegalStateException.class, () -> bootstrap.onApplicationReady());
    }
}


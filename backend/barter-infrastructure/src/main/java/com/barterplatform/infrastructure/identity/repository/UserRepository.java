package com.barterplatform.infrastructure.identity.repository;

import com.barterplatform.domain.identity.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUuid(UUID uuid);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUsername(String username);

    @Query(
            """
            select u.id
            from UserEntity u
            where lower(u.username) like concat('%', :query, '%')
               or lower(u.email) like concat('%', :query, '%')
            """)
    List<Long> findIdsByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(@Param("query") String query);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}


package com.kjt.lms.repository;

import com.kjt.lms.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmailAndDeletedFalse(String email);

    @Query("""
        SELECT u.id AS id,
               u.email AS email,
               u.password AS password,
               u.isVerified AS verified,
               u.isLocked AS locked,
               r.code AS roleCode
        FROM UserEntity u
        JOIN RoleEntity r ON u.roleId = r.id
        WHERE u.email = :email
          AND u.deleted = false
          AND r.deleted = false
        """)
    Optional<UserAuthProjection> findAuthByEmail(@Param("email") String email);

    long countByDeletedFalse();

    @Query("""
            SELECT COUNT(u)
            FROM UserEntity u, RoleEntity r
            WHERE u.roleId = r.id
              AND r.code = :roleCode
              AND u.deleted = false
              AND r.deleted = false
            """)
    long countByRoleCode(@Param("roleCode") String roleCode);

    @Query("""
            SELECT u
            FROM UserEntity u, RoleEntity r
            WHERE u.roleId = r.id
              AND r.code = 'INSTRUCTOR'
              AND u.deleted = false
              AND r.deleted = false
            """)
    List<UserEntity> findAllInstructors();

    interface UserAuthProjection {
        UUID getId();

        String getEmail();

        String getPassword();

        Boolean getVerified();

        Boolean getLocked();

        String getRoleCode();
    }
}

package com.kjt.lms.repository;

import com.kjt.lms.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmailAndDeletedFalse(String email);

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
}

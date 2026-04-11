package com.kjt.lms.repository;

import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.CourseLevelEnum;
import com.kjt.lms.common.constants.CourseStatusEnum;
import com.kjt.lms.model.entity.CourseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, UUID> {


    Page<CourseEntity> findByInstructorId(UUID instructorId, Pageable pageable);

    /**
     * Find courses by category (published & active)
     */
    @Query("""
                SELECT c
                FROM CourseEntity c
                WHERE c.categoryId = :categoryId
                  AND c.status = :status
                  AND c.active = :active
                  AND c.deleted = false
            """)
    Page<CourseEntity> findCoursesByCategory(
            @Param("categoryId") UUID categoryId,
            @Param("status") CourseStatusEnum status,
            @Param("active") CommonStatusEnum active,
            Pageable pageable
    );


    @Query("""
        SELECT c
        FROM CourseEntity c
        WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:courseStatus IS NULL OR c.status = :courseStatus)
            AND (:courseLevel IS NULL OR c.level = :courseLevel)
            AND (:active IS NULL OR c.active = :active)
            AND c.deleted = false
        order by c.id desc
    """)
    Page<CourseEntity> search(
            @Param("keyword") String keyword,
            @Param("courseStatus") CourseStatusEnum courseStatus,
            @Param("courseLevel") CourseLevelEnum courseLevel,
            @Param("active") CommonStatusEnum active,
            Pageable pageable
    );


    boolean existsByCategoryIdAndDeletedFalse(UUID categoryId);
}

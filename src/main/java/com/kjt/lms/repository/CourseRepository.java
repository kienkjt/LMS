package com.kjt.lms.repository;

import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.CourseLevelEnum;
import com.kjt.lms.common.constants.CourseStatusEnum;
import com.kjt.lms.model.entity.CourseEntity;
import com.kjt.lms.model.response.course.CourseCreateResponseDto;
import com.kjt.lms.model.response.course.CourseListItemResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, UUID> {

    Optional<CourseEntity> findByIdAndDeletedFalse(UUID id);

    @Query("""
            SELECT new com.kjt.lms.model.response.course.CourseListItemResponseDto(
                c.id,
                c.title,
                c.shortDescription,
                c.thumbnail,
                c.price,
                c.discountPrice,
                c.level,
                c.status,
                c.totalDuration,
                c.totalLessons,
                c.totalStudents,
                c.avgRating,
                c.totalReviews,
                c.language,
                c.createdAt,
                u.fullName
            )
            FROM CourseEntity c
            LEFT JOIN UserEntity u ON u.id = c.instructorId
            WHERE c.instructorId = :instructorId
              AND c.deleted = false
            """)
    Page<CourseListItemResponseDto> findInstructorCoursesWithInstructorName(
            @Param("instructorId") UUID instructorId,
            Pageable pageable
    );

    /**
     * Find courses by category (published & active)
     */
    @Query("""
                SELECT new com.kjt.lms.model.response.course.CourseListItemResponseDto(
                    c.id,
                    c.title,
                    c.shortDescription,
                    c.thumbnail,
                    c.price,
                    c.discountPrice,
                    c.level,
                    c.status,
                    c.totalDuration,
                    c.totalLessons,
                    c.totalStudents,
                    c.avgRating,
                    c.totalReviews,
                    c.language,
                    c.createdAt,
                    u.fullName
                )
                FROM CourseEntity c
                LEFT JOIN UserEntity u ON u.id = c.instructorId
                WHERE c.categoryId = :categoryId
                  AND c.status IN :statuses
                  AND c.active = :active
                  AND c.deleted = false
            """)
    Page<CourseListItemResponseDto> findCoursesByCategoryWithInstructorName(
            @Param("categoryId") UUID categoryId,
            @Param("statuses") Collection<CourseStatusEnum> statuses,
            @Param("active") CommonStatusEnum active,
            Pageable pageable
    );


    @Query("""
        SELECT new com.kjt.lms.model.response.course.CourseListItemResponseDto(
            c.id,
            c.title,
            c.shortDescription,
            c.thumbnail,
            c.price,
            c.discountPrice,
            c.level,
            c.status,
            c.totalDuration,
            c.totalLessons,
            c.totalStudents,
            c.avgRating,
            c.totalReviews,
            c.language,
            c.createdAt,
            u.fullName
        )
        FROM CourseEntity c
        LEFT JOIN UserEntity u ON u.id = c.instructorId
        WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:courseStatus IS NULL OR c.status = :courseStatus)
            AND (:courseLevel IS NULL OR c.level = :courseLevel)
            AND (:active IS NULL OR c.active = :active)
            AND c.deleted = false
        order by c.id desc
    """)
    Page<CourseListItemResponseDto> searchWithInstructorName(
            @Param("keyword") String keyword,
            @Param("courseStatus" ) CourseStatusEnum courseStatus,
            @Param("courseLevel") CourseLevelEnum courseLevel,
            @Param("active") CommonStatusEnum active,
            Pageable pageable
    );

    @Query("""
        SELECT new com.kjt.lms.model.response.course.CourseListItemResponseDto(
            c.id,
            c.title,
            c.shortDescription,
            c.thumbnail,
            c.price,
            c.discountPrice,
            c.level,
            c.status,
            c.totalDuration,
            c.totalLessons,
            c.totalStudents,
            c.avgRating,
            c.totalReviews,
            c.language,
            c.createdAt,
            u.fullName
        )
        FROM CourseEntity c
        LEFT JOIN UserEntity u ON u.id = c.instructorId
        WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND c.status IN :statuses
            AND c.active = :active
            AND c.deleted = false
        ORDER BY c.createdAt DESC
    """)
    Page<CourseListItemResponseDto> searchPublicWithInstructorName(
            @Param("keyword") String keyword,
            @Param("statuses") Collection<CourseStatusEnum> statuses,
            @Param("active") CommonStatusEnum active,
            Pageable pageable
    );

    @Query("""
        SELECT new com.kjt.lms.model.response.course.CourseListItemResponseDto(
            c.id,
            c.title,
            c.shortDescription,
            c.thumbnail,
            c.price,
            c.discountPrice,
            c.level,
            c.status,
            c.totalDuration,
            c.totalLessons,
            c.totalStudents,
            c.avgRating,
            c.totalReviews,
            c.language,
            c.createdAt,
            u.fullName
        )
        FROM CourseEntity c
        LEFT JOIN UserEntity u ON u.id = c.instructorId
        WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:courseStatus IS NULL OR c.status = :courseStatus)
            AND (:courseLevel IS NULL OR c.level = :courseLevel)
            AND (:active IS NULL OR c.active = :active)
            AND (:instructorId IS NULL OR c.instructorId = :instructorId)
            AND c.deleted = false
        ORDER BY c.createdAt DESC
    """)
    Page<CourseListItemResponseDto> searchManagedWithInstructorName(
            @Param("keyword") String keyword,
            @Param("courseStatus") CourseStatusEnum courseStatus,
            @Param("courseLevel") CourseLevelEnum courseLevel,
            @Param("active") CommonStatusEnum active,
            @Param("instructorId") UUID instructorId,
            Pageable pageable
    );

    @Query("""
        SELECT new com.kjt.lms.model.response.course.CourseListItemResponseDto(
            c.id,
            c.title,
            c.shortDescription,
            c.thumbnail,
            c.price,
            c.discountPrice,
            c.level,
            c.status,
            c.totalDuration,
            c.totalLessons,
            c.totalStudents,
            c.avgRating,
            c.totalReviews,
            c.language,
            c.createdAt,
            u.fullName
        )
        FROM CourseEntity c
        LEFT JOIN UserEntity u ON u.id = c.instructorId
        WHERE c.status IN :statuses
            AND c.active = :active
            AND c.deleted = false
        ORDER BY COALESCE(c.avgRating, 0) DESC, COALESCE(c.totalReviews, 0) DESC, c.createdAt DESC
    """)
    Page<CourseListItemResponseDto> findTopRatedPublicCourses(
            @Param("statuses") Collection<CourseStatusEnum> statuses,
            @Param("active") CommonStatusEnum active,
            Pageable pageable
    );

    @Query("""
        SELECT new com.kjt.lms.model.response.course.CourseListItemResponseDto(
            c.id,
            c.title,
            c.shortDescription,
            c.thumbnail,
            c.price,
            c.discountPrice,
            c.level,
            c.status,
            c.totalDuration,
            c.totalLessons,
            c.totalStudents,
            c.avgRating,
            c.totalReviews,
            c.language,
            c.createdAt,
            u.fullName
        )
        FROM CourseEntity c
        LEFT JOIN UserEntity u ON u.id = c.instructorId
        WHERE c.status IN :statuses
            AND c.active = :active
            AND c.deleted = false
        ORDER BY COALESCE(c.totalStudents, 0) DESC, COALESCE(c.avgRating, 0) DESC, c.createdAt DESC
    """)
    Page<CourseListItemResponseDto> findTrendingPublicCourses(
            @Param("statuses") Collection<CourseStatusEnum> statuses,
            @Param("active") CommonStatusEnum active,
            Pageable pageable
    );

    boolean existsByCategoryIdAndDeletedFalse(UUID categoryId);

    @Query("""
            SELECT new com.kjt.lms.model.response.course.CourseCreateResponseDto(
                c.id,
                c.instructorId,
                u.fullName,
                c.title,
                c.status,
                c.createdAt
            )
            FROM CourseEntity c
            LEFT JOIN UserEntity u ON u.id = c.instructorId
            WHERE c.id = :courseId
            """)
    Optional<CourseCreateResponseDto> findCreateResponseById(@Param("courseId") UUID courseId);

    @Query("""
            SELECT u.fullName
            FROM CourseEntity c, UserEntity u
            WHERE c.id = :courseId
              AND u.id = c.instructorId
            """)
    Optional<String> findInstructorNameByCourseId(@Param("courseId") UUID courseId);
}

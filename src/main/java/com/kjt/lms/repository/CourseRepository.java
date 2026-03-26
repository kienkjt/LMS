package com.kjt.lms.repository;

import com.kjt.lms.common.constants.CommonStatusEnum;
import com.kjt.lms.common.constants.CourseStatusEnum;
import com.kjt.lms.model.entity.CourseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, UUID> {

    /**
     * Find course by slug
     */
    Optional<CourseEntity> findBySlug(String slug);

    /**
     * Find course by ID
     */
    Optional<CourseEntity> findById(UUID id);

    /**
     * Find all courses by instructor ID
     */
    Page<CourseEntity> findByInstructorId(UUID instructorId, Pageable pageable);

    /**
     * Find all published courses
     */
    Page<CourseEntity> findByStatusAndActive(CourseStatusEnum status, CommonStatusEnum active, Pageable pageable);

    /**
     * Find courses by category
     */
    Page<CourseEntity> findByCategoryIdAndStatusAndActive(UUID categoryId, CourseStatusEnum status, CommonStatusEnum active, Pageable pageable);

    /**
     * Search courses by title
     */
    @Query("SELECT c FROM CourseEntity c WHERE c.active = :active AND c.status = :status AND LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<CourseEntity> searchByTitle(@Param("keyword") String keyword, @Param("status") CourseStatusEnum status, @Param("active") CommonStatusEnum active, Pageable pageable);

    /**
     * Find all draft courses for instructor
     */
    List<CourseEntity> findByInstructorIdAndStatusAndActive(UUID instructorId, CourseStatusEnum status, CommonStatusEnum active);

    /**
     * Check if slug already exists for different course
     */
    @Query("SELECT COUNT(c) > 0 FROM CourseEntity c WHERE c.slug = :slug AND c.id != :courseId")
    boolean existsBySlugAndNotId(@Param("slug") String slug, @Param("courseId") UUID courseId);

    /**
     * Check if slug exists
     */
    boolean existsBySlug(String slug);

    /**
     * Find courses with avg rating
     */
    @Query("SELECT c FROM CourseEntity c WHERE c.active = :active AND c.status = :status ORDER BY c.avgRating DESC")
    Page<CourseEntity> findTopRatedCourses(@Param("status") CourseStatusEnum status, @Param("active") CommonStatusEnum active, Pageable pageable);

    /**
     * Find trending courses (by total students)
     */
    @Query("SELECT c FROM CourseEntity c WHERE c.active = :active AND c.status = :status ORDER BY c.totalStudents DESC")
    Page<CourseEntity> findTrendingCourses(@Param("status") CourseStatusEnum status, @Param("active") CommonStatusEnum active, Pageable pageable);
}


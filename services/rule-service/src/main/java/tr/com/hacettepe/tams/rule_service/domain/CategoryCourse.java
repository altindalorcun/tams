package tr.com.hacettepe.tams.rule_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Junction entity for the many-to-many relationship between
 * {@link Category} and {@link Course}.
 *
 * <p>Carries {@code isMandatory}: when true and the student's enrollment cohort falls
 * within the applicability range, the student must pass this course.
 *
 * <p>Applicability bounds ({@code appliesFromYear/Term}, {@code appliesToYear/Term})
 * control whether this course assignment is active for a given enrollment cohort:
 * <ul>
 *   <li>{@code appliesFrom} (inclusive): first cohort for which the assignment applies.</li>
 *   <li>{@code appliesTo} (exclusive): first cohort for which the assignment no longer applies.</li>
 * </ul>
 * When the student's cohort is outside the range, the course is ignored entirely in
 * category evaluation (neither counted nor required).
 */
@Entity
@Table(name = "category_courses")
@Getter
@Setter
@NoArgsConstructor
public class CategoryCourse {

    @EmbeddedId
    private CategoryCourseId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("courseId")
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "is_mandatory", nullable = false)
    private boolean isMandatory = false;

    @Column(name = "applies_from_year")
    private Integer appliesFromYear;

    /** {@code GUZ} or {@code BAHAR}; null defaults to {@code GUZ} at evaluation time. */
    @Column(name = "applies_from_term", length = 10)
    private String appliesFromTerm;

    @Column(name = "applies_to_year")
    private Integer appliesToYear;

    /** {@code GUZ} or {@code BAHAR}; null defaults to {@code GUZ} at evaluation time. */
    @Column(name = "applies_to_term", length = 10)
    private String appliesToTerm;

    public CategoryCourse(Category category, Course course, boolean isMandatory) {
        this.id = new CategoryCourseId(category.getId(), course.getId());
        this.category = category;
        this.course = course;
        this.isMandatory = isMandatory;
    }
}

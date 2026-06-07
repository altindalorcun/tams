package tr.com.hacettepe.tams.rule_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Junction entity for the many-to-many relationship between
 * {@link Category} and {@link Course}.
 * Carries the extra {@code isMandatory} attribute: when true, the student must
 * pass this specific course regardless of other category thresholds.
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

    public CategoryCourse(Category category, Course course, boolean isMandatory) {
        this.id = new CategoryCourseId(category.getId(), course.getId());
        this.category = category;
        this.course = course;
        this.isMandatory = isMandatory;
    }
}

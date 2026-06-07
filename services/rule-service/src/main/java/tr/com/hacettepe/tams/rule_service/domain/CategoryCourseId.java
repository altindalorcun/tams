package tr.com.hacettepe.tams.rule_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite primary key for the {@code category_courses} junction table.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class CategoryCourseId implements Serializable {

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "course_id")
    private UUID courseId;

    public CategoryCourseId(UUID categoryId, UUID courseId) {
        this.categoryId = categoryId;
        this.courseId = courseId;
    }
}

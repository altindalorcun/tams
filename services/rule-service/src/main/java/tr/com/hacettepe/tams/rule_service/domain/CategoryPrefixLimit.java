package tr.com.hacettepe.tams.rule_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Limits how many courses with a given code prefix can count towards
 * satisfying a {@link Category}'s thresholds.
 *
 * <p>Example: {@code courseCodePrefix = "SEC", maxCount = 3} means at most
 * 3 "SEC…" courses are counted even if the student passed more.
 */
@Entity
@Table(name = "category_prefix_limits")
@Getter
@Setter
@NoArgsConstructor
public class CategoryPrefixLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "course_code_prefix", nullable = false, length = 10)
    private String courseCodePrefix;

    @Column(name = "max_count", nullable = false)
    private int maxCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    public CategoryPrefixLimit(Category category, String courseCodePrefix, int maxCount) {
        this.category = category;
        this.courseCodePrefix = courseCodePrefix;
        this.maxCount = maxCount;
    }
}

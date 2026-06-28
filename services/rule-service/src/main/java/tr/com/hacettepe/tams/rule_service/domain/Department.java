package tr.com.hacettepe.tams.rule_service.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A university department (e.g. "Bilgisayar Mühendisliği").
 * Acts as the root aggregate for graduation rules: each department owns
 * a course pool and a set of graduation categories.
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;

    @Column(name = "min_total_ects", precision = 6, scale = 2)
    private BigDecimal minTotalEcts;

    @Column(name = "block_on_any_f_grade", nullable = false)
    private boolean blockOnAnyFGrade = false;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Category> categories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DepartmentCourse> departmentCourses = new LinkedHashSet<>();

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<CurriculumEquivalenceRule> curriculumEquivalenceRules = new java.util.ArrayList<>();

    public Department(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }

    public Department(String name, String code, String description,
                      BigDecimal minTotalEcts, boolean blockOnAnyFGrade) {
        this.name = name;
        this.code = code;
        this.description = description;
        this.minTotalEcts = minTotalEcts;
        this.blockOnAnyFGrade = blockOnAnyFGrade;
    }
}

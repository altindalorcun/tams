package tr.com.hacettepe.tams.analysis_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EnrollmentCohortComparator}.
 */
class EnrollmentCohortComparatorTest {

    private EnrollmentCohortComparator comparator;

    @BeforeEach
    void setUp() {
        comparator = new EnrollmentCohortComparator();
    }

    @Nested
    @DisplayName("compare")
    class CompareTests {

        @Test
        @DisplayName("2017 BAHAR is before 2017 GUZ within the same year")
        void sameYear_baharBeforeGuz() {
            assertThat(comparator.compare(2017, "BAHAR", 2017, "GUZ")).isNegative();
            assertThat(comparator.compare(2017, "GUZ", 2017, "BAHAR")).isPositive();
        }

        @Test
        @DisplayName("null term defaults to GUZ")
        void nullTerm_defaultsToGuz() {
            assertThat(comparator.compare(2017, null, 2017, "GUZ")).isZero();
        }
    }

    @Nested
    @DisplayName("isApplicable")
    class ApplicableTests {

        @Test
        @DisplayName("exclusive appliesTo: 2017 GUZ enrollment excluded when to=2017 GUZ")
        void appliesToExclusive_excludesFromBoundary() {
            assertThat(comparator.isApplicable(2017, "GUZ", null, null, 2017, "GUZ"))
                    .isFalse();
            assertThat(comparator.isApplicable(2017, "BAHAR", null, null, 2017, "GUZ"))
                    .isTrue();
        }

        @Test
        @DisplayName("inclusive appliesFrom: 2017 GUZ enrollment included when from=2017 GUZ")
        void appliesFromInclusive_includesFromBoundary() {
            assertThat(comparator.isApplicable(2017, "GUZ", 2017, "GUZ", null, null))
                    .isTrue();
            assertThat(comparator.isApplicable(2017, "BAHAR", 2017, "GUZ", null, null))
                    .isFalse();
        }

        @Test
        @DisplayName("null enrollment year ignores all bounds")
        void nullEnrollment_appliesRegardless() {
            assertThat(comparator.isApplicable(null, null, 2017, "GUZ", 2020, "GUZ"))
                    .isTrue();
        }

        @Test
        @DisplayName("no bounds applies to all cohorts")
        void noBounds_alwaysApplicable() {
            assertThat(comparator.isApplicable(2020, "GUZ", null, null, null, null))
                    .isTrue();
        }
    }
}

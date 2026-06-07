package tr.com.hacettepe.tams.auth_service.domain;

/**
 * Represents the set of access roles a user can hold.
 * A single user carries exactly one role throughout their lifetime.
 */
public enum Role {
    ADMIN,
    TEACHER,
    STUDENT
}

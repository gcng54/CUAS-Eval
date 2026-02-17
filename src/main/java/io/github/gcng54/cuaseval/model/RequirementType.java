package io.github.gcng54.cuaseval.model;

/**
 * Requirement type classification per CWA 18150.
 * Functional, Performance, and Testable requirement categories.
 */
public enum RequirementType {
    /** Functional requirement — what the system shall do */
    FUNCTIONAL,
    /** Performance requirement — how well the system shall perform */
    PERFORMANCE,
    /** Testable parameter — measurable/observable test criterion */
    TESTABLE
}

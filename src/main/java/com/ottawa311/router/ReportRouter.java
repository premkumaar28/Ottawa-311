package com.ottawa311.router;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Picks the department for a report category
 */
public final class ReportRouter {

    /** Department used when no strategy supports the category. */
    public static final String DEFAULT_DEPARTMENT = "Roads";

    private static final List<ReportRoutingStrategy> STRATEGIES = Collections.unmodifiableList(
            Arrays.asList(new RoadsRoutingStrategy(), new SidewalksRoutingStrategy()));

    private static final Set<String> VALID_DEPARTMENTS = Collections.unmodifiableSet(
            STRATEGIES.stream()
                    .map(ReportRoutingStrategy::getAssignedDepartment)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));

    private static final Set<String> VALID_CATEGORIES = Collections.unmodifiableSet(
            STRATEGIES.stream()
                    .map(ReportRoutingStrategy::getCategory)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));

    private ReportRouter() {
    }

    /**
     * Resolves the department for a report category.
     *
     * @param category
     */
    public static String route(String category) {
        return STRATEGIES.stream()
                .filter(s -> s.supports(category))
                .map(ReportRoutingStrategy::getAssignedDepartment)
                .findFirst()
                .orElse(DEFAULT_DEPARTMENT);
    }

    /** @return unmodifiable, ordered set of department names supported by the strategies. */
    public static Set<String> getValidDepartments() {
        return VALID_DEPARTMENTS;
    }

    /** @return unmodifiable, ordered set of category names supported by the strategies. */
    public static Set<String> getValidCategories() {
        return VALID_CATEGORIES;
    }

    /** @return true if the department is supported by an active strategy. */
    public static boolean isValidDepartment(String department) {
        return department != null && VALID_DEPARTMENTS.contains(department);
    }
}
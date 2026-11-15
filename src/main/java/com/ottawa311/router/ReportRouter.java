package com.ottawa311.router;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Context manager executing registered report routing strategies.
 */
public class ReportRouter {
	private static final List<ReportRoutingStrategy> STRATEGIES = Collections.unmodifiableList(Arrays.asList(
	        new RoadsRoutingStrategy(),
	        new SidewalksRoutingStrategy()
	    ));

	    private static final Set<String> VALID_DEPARTMENTS = Collections.unmodifiableSet(
	        STRATEGIES.stream()
	                  .map(ReportRoutingStrategy::getAssignedDepartment)
	                  .collect(Collectors.toSet())
	    );

	    private ReportRouter() {}

	    /**
	     * Resolves the target department for a given report category.
	     *
	     * @param category Report category submitted by the resident
	     * @return Assigned department name (defaults to "Roads")
	     */
	    public static String route(String category) {
	        return STRATEGIES.stream()
	                         .filter(s -> s.supports(category))
	                         .map(ReportRoutingStrategy::getAssignedDepartment)
	                         .findFirst()
	                         .orElse("Roads");
	    }

	    /**
	     * Returns an unmodifiable set of all valid department names supported by active strategies.
	     *
	     * @return Set of valid department strings
	     */
	    public static Set<String> getValidDepartments() {
	        return VALID_DEPARTMENTS;
	    }

	    /**
	     * Validates whether a given department name is supported.
	     *
	     * @param department Department name to check
	     * @return true if valid, false otherwise
	     */
	    public static boolean isValidDepartment(String department) {
	        return department != null && VALID_DEPARTMENTS.contains(department);
	    }
}
package com.ottawa311.router;

/**
 * Strategy pattern interface for routing infrastructure reports to appropriate departments.
 */
public interface ReportRoutingStrategy {
    /**
     * @return the department name responsible for handling the report category.
     */
    String getAssignedDepartment();

    /**
     * Checks if this strategy supports the given report category.
     * @param category the category string from the report form
     * @return true if supported, false otherwise
     */
    boolean supports(String category);
}

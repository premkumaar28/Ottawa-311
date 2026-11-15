package com.ottawa311.router;

/**
 * Strategy pattern interface for routing infrastructure reports to appropriate departments.
 */
public interface ReportRoutingStrategy {
    /**
     * @return the department name responsible for handling the report category.
     */
    String getAssignedDepartment();
    
    /** @return the  category name shown on the report form (e.g. "Road"). */
    String getCategory();

    /**
     * Checks if this strategy supports the given report category.
     * @param category the category string from the report form
     * @return true if supported, false otherwise
     */
    boolean supports(String category);
}

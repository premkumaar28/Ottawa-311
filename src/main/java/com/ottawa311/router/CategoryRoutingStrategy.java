package com.ottawa311.router;

/**
 * Category-based routing strategies
 */
public abstract class CategoryRoutingStrategy implements ReportRoutingStrategy {

    private final String department;
    private final String category;

    protected CategoryRoutingStrategy(String department, String category) {
        if (department == null || department.trim().isEmpty()
                || category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("department and category are required");
        }
        this.department = department;
        this.category = category;
    }

	@Override
    public String getAssignedDepartment() {
        return department;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public boolean supports(String candidate) {
        if (candidate == null) {
            return false;
        }
        String value = candidate.trim();
        return category.equalsIgnoreCase(value) || department.equalsIgnoreCase(value);
    }
}
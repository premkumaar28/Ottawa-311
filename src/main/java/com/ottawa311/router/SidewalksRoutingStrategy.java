//package com.ottawa311.router;
//
///**
// * Concrete strategy for routing Sidewalk-related infrastructure reports to the Sidewalks department.
// */
//public class SidewalksRoutingStrategy implements ReportRoutingStrategy {
//
//    @Override
//    public String getAssignedDepartment() {
//        return "Sidewalks";
//    }
//
//    @Override
//    public boolean supports(String category) {
//        return "Sidewalk".equalsIgnoreCase(category) || "Sidewalks".equalsIgnoreCase(category);
//    }
//}

package com.ottawa311.router;

/** Routes "Sidewalk" reports to the Sidewalks department. */
public class SidewalksRoutingStrategy extends CategoryRoutingStrategy {

    public SidewalksRoutingStrategy() {
        super("Sidewalks", "Sidewalk");
    }
}
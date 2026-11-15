//package com.ottawa311.router;
//
///**
// * Concrete strategy for routing Road-related infrastructure reports to the Roads department.
// */
//public class RoadsRoutingStrategy implements ReportRoutingStrategy {
//
//    @Override
//    public String getAssignedDepartment() {
//        return "Roads";
//    }
//
//    @Override
//    public boolean supports(String category) {
//        return "Road".equalsIgnoreCase(category) || "Roads".equalsIgnoreCase(category);
//    }
//}

package com.ottawa311.router;

/** Routes "Road" reports to the Roads department. */
public class RoadsRoutingStrategy extends CategoryRoutingStrategy {

    public RoadsRoutingStrategy() {
        super("Roads", "Road");
    }
}
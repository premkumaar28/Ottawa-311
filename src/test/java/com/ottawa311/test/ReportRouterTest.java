package com.ottawa311.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import com.ottawa311.router.CategoryRoutingStrategy;
import com.ottawa311.router.ReportRouter;
import com.ottawa311.router.ReportRoutingStrategy;
import com.ottawa311.router.RoadsRoutingStrategy;
import com.ottawa311.router.SidewalksRoutingStrategy;

public class ReportRouterTest {

    @Test
    public void routesRoadCategoryToRoads() {
        assertEquals("Roads", ReportRouter.route("Road"));
    }

    @Test
    public void routesSidewalkCategoryToSidewalks() {
        assertEquals("Sidewalks", ReportRouter.route("Sidewalk"));
    }

    @Test
    public void routingIgnoresCaseAndSurroundingSpaces() {
        assertEquals("Sidewalks", ReportRouter.route("  sIdEwAlK "));
    }

    @Test
    public void routingAcceptsDepartmentNameAsCategory() {
        assertEquals("Sidewalks", ReportRouter.route("Sidewalks"));
        assertEquals("Roads", ReportRouter.route("ROADS"));
    }

    @Test
    public void unknownOrNullCategoryFallsBackToDefaultDepartment() {
        assertEquals(ReportRouter.DEFAULT_DEPARTMENT, ReportRouter.route("Potholes"));
        assertEquals(ReportRouter.DEFAULT_DEPARTMENT, ReportRouter.route(null));
        assertEquals(ReportRouter.DEFAULT_DEPARTMENT, ReportRouter.route(""));
    }

    @Test
    public void validDepartmentsKeepRegistrationOrder() {
        List<String> departments = new ArrayList<>(ReportRouter.getValidDepartments());
        assertEquals(Arrays.asList("Roads", "Sidewalks"), departments);
    }

    @Test
    public void validCategoriesKeepRegistrationOrder() {
        List<String> categories = new ArrayList<>(ReportRouter.getValidCategories());
        assertEquals(Arrays.asList("Road", "Sidewalk"), categories);
    }

    @Test
    public void isValidDepartmentIsExactAndNullSafe() {
        assertTrue(ReportRouter.isValidDepartment("Roads"));
        assertTrue(ReportRouter.isValidDepartment("Sidewalks"));
        assertFalse(ReportRouter.isValidDepartment("roads"));      // exact match only
        assertFalse(ReportRouter.isValidDepartment("Parks"));
        assertFalse(ReportRouter.isValidDepartment(null));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void validDepartmentsCannotBeModified() {
        ReportRouter.getValidDepartments().add("Parks");
    }

    @Test
    public void roadsStrategyReportsItsDepartmentAndCategory() {
        ReportRoutingStrategy s = new RoadsRoutingStrategy();
        assertEquals("Roads", s.getAssignedDepartment());
        assertEquals("Road", s.getCategory());
        assertTrue(s.supports("road"));
        assertFalse(s.supports("Sidewalk"));
        assertFalse(s.supports(null));
    }

    @Test
    public void sidewalksStrategyReportsItsDepartmentAndCategory() {
        ReportRoutingStrategy s = new SidewalksRoutingStrategy();
        assertEquals("Sidewalks", s.getAssignedDepartment());
        assertEquals("Sidewalk", s.getCategory());
        assertTrue(s.supports("SIDEWALKS"));
        assertFalse(s.supports("Road"));
        assertFalse(s.supports(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void categoryStrategyRejectsBlankDepartment() {
        new CategoryRoutingStrategy(" ", "Parks") { };
    }

    @Test(expected = IllegalArgumentException.class)
    public void categoryStrategyRejectsNullCategory() {
        new CategoryRoutingStrategy("Parks", null) { };
    }
}
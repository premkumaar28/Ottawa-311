package com.ottawa311.web;

import com.ottawa311.db.ReportDAO;
import com.ottawa311.db.UserDAO.Account;
import com.ottawa311.db.ReportDAO.Report;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Shows a photo only to the resident who owns it or authorized staff. */
@WebServlet("/photo")
public class PhotoServlet extends HttpServlet {

	 /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final ReportDAO reportDAO = new ReportDAO();

	 @Override
	    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

	        if (!Web.require(request, response, "RESIDENT", "WORKER", "SUPERVISOR")) {
	            return;
	        }

	        Account account = Web.user(request);
	        Report report = reportDAO.get(request.getParameter("id"));

	        if (report == null || report.photoName.isEmpty()) {
	            response.sendError(404, "This report has no photo");
	            return;
	        }

	        boolean supervisor = "SUPERVISOR".equals(account.role);
	        boolean owner = "RESIDENT".equals(account.role) && account.email.equals(report.owner);
	        boolean assignedWorker = "WORKER".equals(account.role) && account.department.equals(report.department);

	        if (!supervisor && !owner && !assignedWorker) {
	            response.sendError(403, "You cannot view this photo");
	            return;
	        }

	        byte[] photoBytes;
	        try {
	            photoBytes = PhotoStorage.load(getServletContext(), report.photoName);
	        } catch (IOException exception) {
	            exception.printStackTrace();
	            response.sendError(404, "Photo file not found");
	            return;
	        }

	        String contentType = report.photoName.endsWith(".png")
	                ? "image/png" : "image/jpeg";

	        response.setContentType(contentType);
	        response.setHeader("X-Content-Type-Options", "nosniff");
	        response.setHeader("Cache-Control", "private, no-store");
	        response.setContentLength(photoBytes.length);
	        response.getOutputStream().write(photoBytes);
	    }
	
}

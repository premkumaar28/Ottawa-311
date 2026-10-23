package com.ottawa311.web;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/")
public class HomeServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath()+"/reports");
    }
	
}

package com.ottawa311.web;


import com.ottawa311.db.UserDAO;
import com.ottawa311.db.UserDAO.Account;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

/** PR0001: registration, login, logout and sessions. Authentication adapter is temporary. */


@WebServlet("/auth")
public class AuthServlet extends HttpServlet {
	
	private final UserDAO userDAO = new UserDAO();
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Show the sign-in and registration form.
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if(Web.user(req) != null) {
            resp.sendRedirect(req.getContextPath()+"/reports");
            return;
        }
        resp.setContentType("text/html;charset=UTF-8");
        String action=req.getContextPath()+"/auth";
        StringBuilder out = new StringBuilder(Web.start(req,"Sign in or register"));
        if("invalid".equals(req.getParameter("error")))out.append("<p class='error'>Invalid email or password.</p>");
        if("exists".equals(req.getParameter("error")))out.append("<p class='error'>That email is already registered.</p>");
        if("input".equals(req.getParameter("error")))out.append("<p class='error'>Enter a valid email and a password between 8 and 128 characters.</p>");
        if("db".equals(req.getParameter("error")))out.append("<p class='error'>Database connection error. Please try again.</p>");
        out.append("<p class='muted'>Demo accounts: resident@example.test / resident123; worker@example.test / worker123; sidewalkworker@example.test / worker123; supervisor@example.test / supervisor123.</p>");
        out.append("<form method='post' action='").append(action).append("'>").append(Web.hiddenToken(req));
        out.append("<label>Email<input type='email' name='email' autocomplete='username' maxlength='254' required></label>");
        out.append("<label>Password<input type='password' name='password' autocomplete='current-password' minlength='8' maxlength='128' required></label>");
        out.append("<button name='mode' value='login'>Sign in</button><button class='secondary' name='mode' value='register'>Register as resident</button></form>");
        resp.getWriter().print(out.append(Web.end()).toString());
    }
    
    // Process sign-in, registration, or logout.
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        if(!Web.validToken(req)) {
            resp.sendError(403,"Invalid form token");
            return;
        }
        String mode=req.getParameter("mode");
        if("logout".equals(mode)) {
            HttpSession s=req.getSession(false);
            if(s!=null)s.invalidate();
            resp.sendRedirect(req.getContextPath()+"/auth");
            return;
        }
        if(!"login".equals(mode)&&!"register".equals(mode)) {
            resp.sendError(400);
            return;
        }
        String email=req.getParameter("email"),pass=req.getParameter("password");
        if(email==null||!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")||email.length()>254||pass==null||pass.length()<8||pass.length()>128) {
            resp.sendRedirect(req.getContextPath()+"/auth?error=input");
            return;
        }
        email=email.trim().toLowerCase(Locale.ROOT);
        
        
        
        try {
        	if("register".equals(mode)&&!userDAO.register(email,pass)) {
                resp.sendRedirect(req.getContextPath()+"/auth?error=exists");
                return;
            }
        	Account a=userDAO.authenticate(email,pass);
            if(a==null) {
                resp.sendRedirect(req.getContextPath()+"/auth?error=invalid");
                return;
            }
            HttpSession old=req.getSession(false);
            if(old!=null)old.invalidate();
            HttpSession session=req.getSession(true);
            session.setAttribute("account",a);
            session.setMaxInactiveInterval(30*60);
            Web.token(req);
            resp.sendRedirect(req.getContextPath()+"/reports");
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath()+"/auth?error=db");
        }        
        
    }

}

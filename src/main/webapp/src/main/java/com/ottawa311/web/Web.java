package com.ottawa311.web;

import com.ottawa311.db.UserDAO.Account;
import javax.servlet.http.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;


final class Web {
	
	 private Web() {
	 }
	 
	 private static final SecureRandom RANDOM = new SecureRandom();
	 
	// Escape user-provided text before displaying it in HTML.
	 static String esc(String s) {
	  if(s == null) return "";
	  return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
	 }
	 
	 static Account user(HttpServletRequest req) {
	        HttpSession s = req.getSession(false);
	        return s == null?null:(Account)s.getAttribute("account");
	 }
	 
	// Check that a user is logged in and has an allowed role.
	static boolean require(HttpServletRequest req,HttpServletResponse resp,String...roles)throws IOException {
		Account a = user(req);
	    if(a == null) {
	        resp.sendRedirect(req.getContextPath()+"/auth");
	        return false;
	    }
	    for(String role:roles)if(role.equals(a.role))return true;
	     resp.sendError(403,"Access denied");
	     return false;
	   }
	
	static String token(HttpServletRequest req) {
        HttpSession s = req.getSession(true);
        String t = (String)s.getAttribute("csrf");
        if(t == null) {
            byte[] b = new byte[24];
            RANDOM.nextBytes(b);
            t = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
            s.setAttribute("csrf",t);
        }
        return t;
    }
	
	static String hiddenToken(HttpServletRequest req) {
	       return "<input type='hidden' name='csrf' value='"+esc(token(req))+"'>";
	   }
	    static boolean validToken(HttpServletRequest req) {
	        HttpSession s = req.getSession(false);
	        if(s == null)return false;
	        String expected = (String)s.getAttribute("csrf"),given=req.getParameter("csrf");
	        return expected != null&&given!=null&&java.security.MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),given.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	    }
	    static String start(HttpServletRequest req,String title) {
	        String root = req.getContextPath();
	        Account a = user(req);
	        String nav = a==null?"<a href='"+root+"/auth'>Sign in</a>":"<a href='"+root+"/reports'>Reports</a> <form class='nav-form' method='post' action='"+root+"/auth'>"+hiddenToken(req)+"<button name='mode' value='logout'>Log out</button></form>";
	        return "<!doctype html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>"+esc(title)+" | Ottawa 311</title><link rel='stylesheet' href='"+root+"/assets/site.css'></head><body><header><strong>Ottawa 311 — Student MVP</strong><nav>"+nav+"</nav></header><main><h1>"+esc(title)+"</h1>";
	    }
	    static String end() {
	        return "</main><footer>Educational demonstration — not an official City of Ottawa service.</footer></body></html>";
	    }
	
}

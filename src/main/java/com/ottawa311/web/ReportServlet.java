package com.ottawa311.web;

import com.ottawa311.db.ReportDAO;
import com.ottawa311.db.ReportDAO.Report;
import com.ottawa311.db.UserDAO.Account;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
/** Person 1 presentation layer: PR0002, PR0003, PR0005-PR0009, PR0011, PR0012. */
@WebServlet("/reports")
@MultipartConfig(maxFileSize=3*1024*1024,maxRequestSize=4*1024*1024)
public class ReportServlet extends HttpServlet {
    /**
	 * 
	 */
	
	private final ReportDAO reportDAO = new ReportDAO();
	
	private static final long serialVersionUID = 1L;
	private static final Set<String> STATUSES=new HashSet<>(Arrays.asList("SUBMITTED","IN_PROGRESS","RESOLVED"));
    private static final Set<String> DEPARTMENTS=new HashSet<>(Arrays.asList("Roads","Sidewalks"));
    private boolean visible(Account a,Report r) {
        return "SUPERVISOR".equals(a.role)||("RESIDENT".equals(a.role)&&r.owner.equals(a.email))||("WORKER".equals(a.role)&&r.department.equals(a.department));
    }
    private String opt(String value,String selected) {
        return "<option value='"+Web.esc(value)+"'"+(value.equals(selected)?" selected":"")+">"+Web.esc(value)+"</option>";
    }
    @Override
    // Display the report form and the reports this user is allowed to see.
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException {
        if(!Web.require(req,resp,"RESIDENT","WORKER","SUPERVISOR"))return;
        Account a=Web.user(req);
        String root=req.getContextPath(),csrf=Web.hiddenToken(req);
        StringBuilder out=new StringBuilder(Web.start(req,"Infrastructure reports"));
        out.append("<p class='muted'>Signed in as ").append(Web.esc(a.email)).append(" — ").append(Web.esc(a.role));
        if("WORKER".equals(a.role))out.append(" (").append(Web.esc(a.department)).append(")");
        out.append("</p>");
        String message=req.getParameter("message");
        if("created".equals(message))out.append("<p class='success'>Report submitted. Your tracking number appears below.</p>");
        if("updated".equals(message))out.append("<p class='success'>Report updated.</p>");
        if("reassigned".equals(message))out.append("<p class='success'>Report reassigned.</p>");
        if("RESIDENT".equals(a.role)) {
            out.append("<section><h2>File a report</h2><form method='post' action='").append(root).append("/reports' enctype='multipart/form-data'>").append(csrf);
            out.append("<input type='hidden' name='action' value='create'><label>Location / address<input name='location' maxlength='200' required></label>");
            out.append("<label>Category<select name='category'><option>Road</option><option>Sidewalk</option></select></label>");
            out.append("<label>Description<textarea name='description' rows='4' maxlength='1000' required></textarea></label>");
            out.append("<label>Urgency<select name='urgency'><option>Routine</option><option>Urgent</option></select></label>");
            out.append("<label>Optional photo (JPEG/PNG, max 3 MB)<input type='file' name='photo' accept='image/jpeg,image/png'></label>");
            out.append("<button type='submit'>Submit report</button></form></section>");
        }
        out.append("<section><h2>").append("RESIDENT".equals(a.role)?"My reports":"WORKER".equals(a.role)?"Department queue":"Supervisor queue (urgent first, then oldest)").append("</h2>");
        out.append("<div class='table-wrap'><table><thead><tr><th>Tracking</th><th>Location / issue</th><th>Category / department</th><th>Urgency</th><th>Status</th><th>Submitted / closed</th><th>Notes</th><th>Actions</th></tr></thead><tbody>");
        int count=0;
        try {
        List<Report> reports = reportDAO.list();
        for(Report r: reports)if(visible(a,r)) {
            count++;
            out.append("<tr><td><strong>").append(Web.esc(r.id)).append("</strong></td><td>").append(Web.esc(r.location)).append("<p>").append(Web.esc(r.description)).append("</p>");
            if(!r.photoName.isEmpty())out.append("<a href='").append(root).append("/photo?id=").append(Web.esc(r.id)).append("' target='_blank' rel='noopener'>View photo</a>");
            out.append("</td><td>").append(Web.esc(r.category)).append(" / ").append(Web.esc(r.department)).append("</td><td>").append(Web.esc(r.urgency)).append("</td><td>").append(Web.esc(r.status)).append("</td><td>").append(Web.esc(DateTimeFormatter.ISO_INSTANT.format(r.submitted)));
            if(r.closed!=null)out.append("<p>Closed: ").append(Web.esc(r.closed.toString())).append("</p>");
            out.append("</td><td>").append(Web.esc(r.note)).append("</td><td>");
            if(!"RESIDENT".equals(a.role)) {
                out.append("<form method='post' action='").append(root).append("/reports'>").append(csrf).append("<input type='hidden' name='action' value='update'><input type='hidden' name='id' value='").append(Web.esc(r.id)).append("'>");
                out.append("<label>Status<select name='status'>");
                for(String s:Arrays.asList("SUBMITTED","IN_PROGRESS","RESOLVED"))out.append(opt(s,r.status));
                out.append("</select></label><label>Note<input name='note' maxlength='500' value='").append(Web.esc(r.note)).append("'></label><button>Save update</button></form>");
            }
            if("SUPERVISOR".equals(a.role)) {
                out.append("<form method='post' action='").append(root).append("/reports'>").append(csrf).append("<input type='hidden' name='action' value='reassign'><input type='hidden' name='id' value='").append(Web.esc(r.id)).append("'><label>Assign to<select name='department'>");
                out.append(opt("Roads",r.department)).append(opt("Sidewalks",r.department)).append("</select></label><button class='secondary'>Reassign</button></form>");
            }
            out.append("</td></tr>");
        }
        if(count==0)out.append("<tr><td colspan='8'>No reports to display.</td></tr>");
        }catch (SQLException e) {
            e.printStackTrace();
            out.append("<tr><td colspan='8' class='error'>Error loading reports from database.</td></tr>");
        }
        out.append("</tbody></table></div></section>");
        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().print(out.append(Web.end()).toString());
    }
    @Override
    // Handle creating, updating, and reassigning reports.
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException,ServletException {
        if(!Web.require(req,resp,"RESIDENT","WORKER","SUPERVISOR"))return;
        req.setCharacterEncoding("UTF-8");
        if(!Web.validToken(req)) {
            resp.sendError(403,"Invalid form token");
            return;
        }
        Account a=Web.user(req);
        String action=req.getParameter("action"),message;
        if("create".equals(action)) {
            if(!"RESIDENT".equals(a.role)) {
                resp.sendError(403);
                return;
            }
            String loc=req.getParameter("location"),cat=req.getParameter("category"),desc=req.getParameter("description"),urg=req.getParameter("urgency");
            if(loc==null||loc.trim().isEmpty()||loc.length()>200||desc==null||desc.trim().isEmpty()||desc.length()>1000||!Arrays.asList("Road","Sidewalk").contains(cat)||!Arrays.asList("Routine","Urgent").contains(urg)) {
                resp.sendError(400,"Invalid report details");
                return;
            }
            Part photo=req.getPart("photo");
            byte[] bytes=null;
            String ext="";
            if(photo!=null&&photo.getSize()>0) {
                if(photo.getSize()>3*1024*1024) {
                    resp.sendError(400,"Photo exceeds 3 MB");
                    return;
                }
                try(InputStream in=photo.getInputStream()) {
                    bytes=readUpTo(in,3*1024*1024+1);
                }
                if(bytes.length>3*1024*1024) {
                    resp.sendError(400,"Photo exceeds 3 MB");
                    return;
                }
                if(isJpeg(bytes)&&"image/jpeg".equalsIgnoreCase(photo.getContentType()))ext="jpg";
                else if(isPng(bytes)&&"image/png".equalsIgnoreCase(photo.getContentType()))ext="png";
                else {
                    resp.sendError(400,"Photo must be JPEG or PNG");
                    return;
                }
            }
            // First save the photo, then create the report.
            // This prevents a report from pointing to a photo that failed to save.
            String storedPhotoName = "";
            if (bytes != null) {
                try {
                    storedPhotoName = PhotoStorage.save(getServletContext(), ext, bytes);
                } catch (IOException exception) {
                    exception.printStackTrace();
                    resp.sendError(500, "Unable to save photo");
                    return;
                }
            }

            try {
            	reportDAO.create(a.email, loc.trim(), cat, desc.trim(), urg, storedPhotoName);
            } catch (RuntimeException exception) {
                // Only remove the photo created by THIS request, if report creation fails.
                if (!storedPhotoName.isEmpty()) {
                    PhotoStorage.delete(getServletContext(), storedPhotoName);
                }
                throw exception;
            }
            message="created";
        } else if("update".equals(action)) {
            if("RESIDENT".equals(a.role)) {
                resp.sendError(403);
                return;
            }
            Report r=reportDAO.get(req.getParameter("id"));
            if(r==null) {
                resp.sendError(404);
                return;
            }
            if(!visible(a,r)) {
                resp.sendError(403);
                return;
            }
            String status=req.getParameter("status"),note=req.getParameter("note");
            if(!STATUSES.contains(status)||note==null||note.length()>500) {
                resp.sendError(400,"Invalid update");
                return;
            }
            reportDAO.update(r,status,note.trim());
            // Integration: Person 2's ResidentNotificationService observes status changes.
            message="updated";
        } else if("reassign".equals(action)) {
            if(!"SUPERVISOR".equals(a.role)) {
                resp.sendError(403);
                return;
            }
            Report r=reportDAO.get(req.getParameter("id"));
            if(r==null) {
                resp.sendError(404);
                return;
            }
            String department=req.getParameter("department");
            if(!DEPARTMENTS.contains(department)) {
                resp.sendError(400);
                return;
            }
            reportDAO.reassign(r,department);
            message="reassigned";
        } else {
            resp.sendError(400);
            return;
        }
        resp.sendRedirect(req.getContextPath()+"/reports?message="+message);
    }
    private static byte[] readUpTo(InputStream in,int max)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] buf=new byte[8192];
        int n;
        while((n=in.read(buf))!=-1) {
            out.write(buf,0,n);
            if(out.size()>max)break;
        }
        return out.toByteArray();
    }
    private static boolean isJpeg(byte[] b) {
        return b.length>=4&&(b[0]&255)==0xff&&(b[1]&255)==0xd8&&(b[b.length-2]&255)==0xff&&(b[b.length-1]&255)==0xd9;
    }
    private static boolean isPng(byte[] b) {
        return b.length>=8&&(b[0]&255)==137&&b[1]==80&&b[2]==78&&b[3]==71&&b[4]==13&&b[5]==10&&b[6]==26&&b[7]==10;
    }
    
}

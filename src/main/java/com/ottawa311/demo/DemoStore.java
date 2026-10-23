package com.ottawa311.demo;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Temporary in-memory adapter for testing Person 1 pages. Replace with team services. */

public final class DemoStore {
	
	private DemoStore() {
	}
	
	public static final class Account {
		public final String email, role, department;
		private final String password;
		//DEMO ONLY: Dont use passwords like this
		Account(String e, String p, String r, String d) {
			email = e;
			password = p;
			role = r;
			department = d;
		}
		public boolean passwordMatches(String candidate) {
			return password.equals(candidate);
		}
	}
	
	public static final class Report {
		public final String id, owner, location, category, description, urgency, photoName;
		public volatile String department, status="SUBMITTED", note="";
		public final Instant submitted=Instant.now();
		public volatile Instant closed;
		Report(String id, String owner, String location, String category, String description, String urgency, String photoName, String department){
			this.id = id;
            this.owner = owner;
            this.location = location;
            this.category = category;
            this.description = description;
            this.urgency = urgency;
            this.photoName = photoName;
            this.department = department;
		}
	}
	
	private static final Map<String,Account> USERS = new ConcurrentHashMap<>();
	private static final Map<String,Report> REPORTS = new ConcurrentHashMap<>();
	private static final AtomicLong IDS = new AtomicLong(1000);
	
	static {
        USERS.put("resident@example.test",new Account("resident@example.test","resident123","RESIDENT",""));
        USERS.put("worker@example.test",new Account("worker@example.test","worker123","WORKER","Roads"));
        USERS.put("sidewalkworker@example.test",new Account("sidewalkworker@example.test","worker123","WORKER","Sidewalks"));
        USERS.put("supervisor@example.test",new Account("supervisor@example.test","supervisor123","SUPERVISOR",""));
    }
	
	// Find a demo account and check its password.
    public static Account authenticate(String email,String pass) {
        if(email==null||pass==null) return null;
        Account a = USERS.get(email.toLowerCase(Locale.ROOT));
        return a != null && a.passwordMatches(pass)?a:null;
    }
    public static synchronized boolean register(String email,String pass) {
        String key = email.toLowerCase(Locale.ROOT);
        if(USERS.containsKey(key)) return false;
        USERS.put(key,new Account(key,pass,"RESIDENT",""));
        return true;
    }
 
    // The photoName parameter is the unique stored filename, not a file extension.
    // Create a temporary report with a unique tracking number.
    public static Report create(String owner,String location,String category,String description,String urgency,String photoName) {
        String department = "Sidewalk".equals(category)?"Sidewalks":"Roads";
        Report r = new Report("OTT-"+IDS.incrementAndGet(),owner,location,category,description,urgency,photoName,department);
        REPORTS.put(r.id,r);
        return r;
    }
    
    public static Report get(String id) {
        return id == null?null:REPORTS.get(id);
    }
    
    public static List<Report> list() {
        List<Report> out = new ArrayList<>(REPORTS.values());
        out.sort(Comparator.comparing((Report r)->!"Urgent".equals(r.urgency)).thenComparing(r->r.submitted));
        return out;
    }
    
    public static synchronized void update(Report r,String status,String note) {
        r.status = status;
        r.note = note;
        r.closed = "RESOLVED".equals(status)?(r.closed==null?Instant.now():r.closed):null;
    }
    
    public static synchronized void reassign(Report r,String department) {
        r.department=department;
    }
    

}

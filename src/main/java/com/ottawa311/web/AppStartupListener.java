package com.ottawa311.web;

import com.ottawa311.db.SchemaInitializer;
import com.ottawa311.service.ResidentNotificationService;
import com.ottawa311.service.StatusChangePublisher;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Runs when Tomcat starts and stops the application.
 * Start: create the MySQL tables if they are missing, and register the notification observer.
 * Stop: shut the notification thread pool down.
 */
@WebListener
public class AppStartupListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppStartupListener.class.getName());

    private ResidentNotificationService notifications;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            boolean created = SchemaInitializer.ensureSchema();
            LOG.info(created ? "Ottawa 311: database tables created from schema.sql"
                             : "Ottawa 311: database tables already exist");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Ottawa 311: cannot reach or prepare the MySQL database. "
                    + "Check that MySQL is running and db.properties has the right URL, user and password.", e);
        }
        notifications = new ResidentNotificationService();
        StatusChangePublisher.register(notifications);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (notifications != null) {
            StatusChangePublisher.unregister(notifications);
            notifications.shutdown();
        }
    }
}

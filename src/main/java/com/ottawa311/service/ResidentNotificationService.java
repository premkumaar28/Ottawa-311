package com.ottawa311.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Observer Pattern (M0002): tells the resident when the status of their report changes (PR0010).
 * The message is built and "sent" on a background thread pool, so the worker's page returns
 * straight away (multi-threading). For the MVP the message is written to the Tomcat log
 * instead of a real mail server, so the app never fails because SMTP is unavailable.
 */
public class ResidentNotificationService implements StatusChangeObserver {

    private static final Logger LOG = Logger.getLogger(ResidentNotificationService.class.getName());

    private final ExecutorService pool = Executors.newFixedThreadPool(3, runnable -> {
        Thread t = new Thread(runnable, "resident-notification");
        t.setDaemon(true);
        return t;
    });

    @Override
    public void onStatusChange(String reportId, String ownerEmail, String oldStatus, String newStatus, String note) {
        try {
            pool.submit(() -> send(reportId, ownerEmail, oldStatus, newStatus, note));
        } catch (RejectedExecutionException e) {
            LOG.log(Level.WARNING, "Notification for {0} was not sent because the service is shut down", reportId);
        }
    }

    private void send(String reportId, String ownerEmail, String oldStatus, String newStatus, String note) {
        String body = "To: " + ownerEmail
                + " | Report " + reportId
                + " | Status changed from " + oldStatus + " to " + newStatus
                + (note == null || note.isEmpty() ? "" : " | Note: " + note);
        LOG.info("[NOTIFICATION THREAD " + Thread.currentThread().getName() + "] " + body);
    }

    /** Stop accepting work and wait briefly for messages already queued. */
    public void shutdown() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

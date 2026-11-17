package com.ottawa311.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Observer pattern (M0002): the concrete observer that tells a resident when the status of
 * their report changes (PR0010).
 * <p>
 * Multi-threading: messages are built and "sent" on a small background pool, so the worker's
 * page returns immediately. The pool uses a bounded queue, so a burst of updates cannot use
 * unlimited memory; if the queue is full the notification is logged as dropped instead of
 * blocking the servlet thread. Threads are daemon threads and never prevent Tomcat from stopping.
 * <p>
 * For the MVP the message is written to the Tomcat log instead of a mail server, so the app
 * never fails because SMTP is unavailable.
 */
public class ResidentNotificationService implements StatusChangeObserver {

    private static final Logger LOG = Logger.getLogger(ResidentNotificationService.class.getName());

    private static final int POOL_SIZE = 3;
    private static final int QUEUE_CAPACITY = 100;
    private static final long SHUTDOWN_WAIT_SECONDS = 5;

    private final ExecutorService pool = new ThreadPoolExecutor(
            POOL_SIZE, POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(QUEUE_CAPACITY),
            new NamedDaemonThreadFactory("resident-notification"));

    @Override
    public void onStatusChange(String reportId, String ownerEmail, String oldStatus, String newStatus, String note) {
        try {
            pool.execute(() -> send(reportId, ownerEmail, oldStatus, newStatus, note));
        } catch (RejectedExecutionException e) {
            LOG.log(Level.WARNING,
                    "Notification for {0} was not sent because the queue is full or the service is shut down",
                    reportId);
        }
    }

    private void send(String reportId, String ownerEmail, String oldStatus, String newStatus, String note) {
        try {
            String body = buildMessage(reportId, ownerEmail, oldStatus, newStatus, note);
            LOG.info("[NOTIFICATION THREAD " + Thread.currentThread().getName() + "] " + body);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Failed to send notification for " + reportId, e);
        }
    }

    /** Builds the notification text */
    public static String buildMessage(String reportId, String ownerEmail, String oldStatus, String newStatus, String note) {
        return "To: " + ownerEmail
                + " | Report " + reportId
                + " | Status changed from " + oldStatus + " to " + newStatus
                + (note == null || note.isEmpty() ? "" : " | Note: " + note);
    }

    /** Stops accepting work and waits briefly for messages already queued.*/
    public void shutdown() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /** Creates numbered daemon threads so log lines are traceable. */
    private static final class NamedDaemonThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        NamedDaemonThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread t = new Thread(runnable, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
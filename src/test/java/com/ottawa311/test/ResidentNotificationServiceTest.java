package com.ottawa311.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ottawa311.service.ResidentNotificationService;

public class ResidentNotificationServiceTest {

    private Logger logger;
    private Handler handler;
    private Level originalLevel;
    private CountDownLatch logged;
    private final AtomicReference<String> lastMessage = new AtomicReference<>();
    private ResidentNotificationService service;

    @Before
    public void setUp() {
        logged = new CountDownLatch(1);
        logger = Logger.getLogger(ResidentNotificationService.class.getName());
        originalLevel = logger.getLevel();
        logger.setLevel(Level.INFO);

        handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() == Level.INFO) {
                    lastMessage.set(record.getMessage());
                    logged.countDown();
                }
            }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(handler);
        service = new ResidentNotificationService();
    }

    @After
    public void tearDown() {
        service.shutdown();
        logger.removeHandler(handler);
        logger.setLevel(originalLevel);
    }

    // ---- buildMessage ----

    @Test
    public void messageContainsRecipientReportAndStatuses() {
        String msg = ResidentNotificationService.buildMessage(
                "OTT-1001", "res@example.test", "SUBMITTED", "IN_PROGRESS", "");
        assertEquals("To: res@example.test | Report OTT-1001 | Status changed from SUBMITTED to IN_PROGRESS", msg);
    }

    @Test
    public void messageIncludesNoteWhenPresent() {
        String msg = ResidentNotificationService.buildMessage(
                "OTT-1", "a@b.ca", "IN_PROGRESS", "RESOLVED", "Pothole filled");
        assertTrue(msg.endsWith(" | Note: Pothole filled"));
    }

    @Test
    public void messageOmitsNoteWhenNullOrEmpty() {
        assertFalse(ResidentNotificationService.buildMessage("OTT-1", "a@b.ca", "A", "B", null).contains("Note"));
        assertFalse(ResidentNotificationService.buildMessage("OTT-1", "a@b.ca", "A", "B", "").contains("Note"));
    }

    // ---- threading and lifecycle ----

    @Test
    public void notificationIsSentOnBackgroundThread() throws InterruptedException {
        service.onStatusChange("OTT-1001", "res@example.test", "SUBMITTED", "RESOLVED", "Done");

        assertTrue("notification was not logged in time", logged.await(2, TimeUnit.SECONDS));
        String message = lastMessage.get();
        assertTrue(message, message.contains("[NOTIFICATION THREAD resident-notification-"));
        assertTrue(message, message.contains("Report OTT-1001"));
    }

    @Test
    public void callAfterShutdownDoesNotThrow() {
        service.shutdown();
        service.onStatusChange("OTT-1", "a@b.ca", "A", "B", "");   // rejected, only logged
    }

    @Test
    public void shutdownCanBeCalledTwice() {
        service.shutdown();
        service.shutdown();
    }
}
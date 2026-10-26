package com.ottawa311.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Multithreaded Observer Service that sends automated status change notifications asynchronously.
 */
public class ResidentNotificationService {

    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(3);

    public static void notifyStatusChange(String reportId, String ownerEmail, String newStatus, String note) {
        THREAD_POOL.submit(() -> {
            try {
                // Simulate asynchronous email delivery processing
                Thread.sleep(500);
                System.out.println("[NOTIFICATION THREAD] Notification sent to " + ownerEmail + 
                                   " | Report: " + reportId + " | Status: " + newStatus + 
                                   " | Note: " + note);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public static void shutdown() {
        THREAD_POOL.shutdown();
    }
}
package com.ottawa311.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer Pattern (M0002): the subject. The servlet calls publish() after a status change
 * and every registered observer is told. Thread-safe, so it can be used from any servlet thread.
 */
public final class StatusChangePublisher {

    private static final List<StatusChangeObserver> OBSERVERS = new CopyOnWriteArrayList<>();

    private StatusChangePublisher() {
    }

    public static void register(StatusChangeObserver observer) {
        if (observer != null && !OBSERVERS.contains(observer)) {
            OBSERVERS.add(observer);
        }
    }

    public static void unregister(StatusChangeObserver observer) {
        OBSERVERS.remove(observer);
    }

    /** Tell every observer about a status change. Does nothing if the status did not change. */
    public static void publish(String reportId, String ownerEmail, String oldStatus, String newStatus, String note) {
        if (newStatus == null || newStatus.equals(oldStatus)) {
            return;
        }
        for (StatusChangeObserver observer : OBSERVERS) {
            try {
                observer.onStatusChange(reportId, ownerEmail, oldStatus, newStatus, note);
            } catch (RuntimeException e) {
                // One failing observer must never break the worker's status update.
                e.printStackTrace();
            }
        }
    }
}

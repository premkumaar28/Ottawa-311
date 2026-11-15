package com.ottawa311.service;

/**
 * Observer Pattern (M0002): anything that wants to react when a report's status changes
 * implements this interface and registers with {@link StatusChangePublisher}.
 */
public interface StatusChangeObserver {
    void onStatusChange(String reportId, String ownerEmail, String oldStatus, String newStatus, String note);
}

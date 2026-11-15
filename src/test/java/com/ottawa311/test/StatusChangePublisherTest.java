package com.ottawa311.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;

import com.ottawa311.service.StatusChangeObserver;
import com.ottawa311.service.StatusChangePublisher;

public class StatusChangePublisherTest {

    /** Test double that records every call it receives. */
    private static class RecordingObserver implements StatusChangeObserver {
        final List<String> calls = new ArrayList<>();

        @Override
        public void onStatusChange(String reportId, String ownerEmail,
                                   String oldStatus, String newStatus, String note) {
            calls.add(reportId + "|" + ownerEmail + "|" + oldStatus + "|" + newStatus + "|" + note);
        }
    }

    private final List<StatusChangeObserver> registered = new ArrayList<>();

    private <T extends StatusChangeObserver> T register(T observer) {
        StatusChangePublisher.register(observer);
        registered.add(observer);
        return observer;
    }

    @After
    public void cleanUp() {
        for (StatusChangeObserver o : registered) {
            StatusChangePublisher.unregister(o);
        }
        registered.clear();
    }

    @Test
    public void notifiesRegisteredObserverWithAllDetails() {
        RecordingObserver observer = register(new RecordingObserver());

        StatusChangePublisher.publish("OTT-1001", "a@b.ca", "SUBMITTED", "IN_PROGRESS", "On it");

        assertEquals(1, observer.calls.size());
        assertEquals("OTT-1001|a@b.ca|SUBMITTED|IN_PROGRESS|On it", observer.calls.get(0));
    }

    @Test
    public void notifiesEveryObserver() {
        RecordingObserver first = register(new RecordingObserver());
        RecordingObserver second = register(new RecordingObserver());

        StatusChangePublisher.publish("OTT-1", "a@b.ca", "SUBMITTED", "RESOLVED", "");

        assertEquals(1, first.calls.size());
        assertEquals(1, second.calls.size());
    }

    @Test
    public void doesNothingWhenStatusDidNotChange() {
        RecordingObserver observer = register(new RecordingObserver());

        StatusChangePublisher.publish("OTT-1", "a@b.ca", "SUBMITTED", "SUBMITTED", "");

        assertTrue(observer.calls.isEmpty());
    }

    @Test
    public void doesNothingWhenNewStatusIsNull() {
        RecordingObserver observer = register(new RecordingObserver());

        StatusChangePublisher.publish("OTT-1", "a@b.ca", "SUBMITTED", null, "");

        assertTrue(observer.calls.isEmpty());
    }

    @Test
    public void registeringSameObserverTwiceNotifiesOnce() {
        RecordingObserver observer = register(new RecordingObserver());
        StatusChangePublisher.register(observer);

        StatusChangePublisher.publish("OTT-1", "a@b.ca", "SUBMITTED", "RESOLVED", "");

        assertEquals(1, observer.calls.size());
    }

    @Test
    public void nullObserverIsIgnored() {
        StatusChangePublisher.register(null);   // must not throw
        StatusChangePublisher.publish("OTT-1", "a@b.ca", "SUBMITTED", "RESOLVED", "");
    }

    @Test
    public void unregisteredObserverIsNoLongerNotified() {
        RecordingObserver observer = register(new RecordingObserver());
        StatusChangePublisher.unregister(observer);

        StatusChangePublisher.publish("OTT-1", "a@b.ca", "SUBMITTED", "RESOLVED", "");

        assertTrue(observer.calls.isEmpty());
    }

    @Test
    public void failingObserverDoesNotStopTheOthers() {
        register(new StatusChangeObserver() {
            @Override
            public void onStatusChange(String id, String owner, String oldS, String newS, String note) {
                throw new IllegalStateException("Failed");
            }
        });
        RecordingObserver healthy = register(new RecordingObserver());

        StatusChangePublisher.publish("OTT-1", "a@b.ca", "SUBMITTED", "RESOLVED", "");

        assertEquals(1, healthy.calls.size());
    }
}
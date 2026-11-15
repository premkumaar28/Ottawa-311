package com.ottawa311.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.Test;

import com.ottawa311.db.UserDAO;

public class AccountTest {

    private static UserDAO.Account account() {
        return new UserDAO.Account("worker@example.test", "worker123", "WORKER", "Roads");
    }

    @Test
    public void correctPasswordMatches() {
        assertTrue(account().passwordMatches("worker123"));
    }

    @Test
    public void wrongPasswordDoesNotMatch() {
        assertFalse(account().passwordMatches("worker124"));
        assertFalse(account().passwordMatches("WORKER123"));
        assertFalse(account().passwordMatches(""));
    }

    @Test
    public void nullCandidateDoesNotMatchAndDoesNotThrow() {
        assertFalse(account().passwordMatches(null));
    }

    @Test
    public void accountWithNullPasswordNeverMatches() {
        UserDAO.Account a = new UserDAO.Account("x@y.ca", null, "RESIDENT", "");
        assertFalse(a.passwordMatches("anything"));
    }
}
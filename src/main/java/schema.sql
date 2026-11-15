-- Ottawa 311 Infrastructure Reporting System - MySQL DDL
-- Safe to run more than once: nothing is dropped and existing rows are kept.
-- The web app also runs this file automatically at startup if the tables are missing.

CREATE DATABASE IF NOT EXISTS ottawa311_db;
USE ottawa311_db;

-- 1. Users (PR0001)
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    role VARCHAR(20) NOT NULL, -- RESIDENT, WORKER or SUPERVISOR
    department VARCHAR(50) NOT NULL DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Reports (PR0002 - PR0012)
CREATE TABLE IF NOT EXISTS reports (
    report_id VARCHAR(30) PRIMARY KEY,
    owner_email VARCHAR(254) NOT NULL,
    location VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    urgency VARCHAR(20) NOT NULL,
    photo_name VARCHAR(100) NOT NULL DEFAULT '',
    department VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    notes VARCHAR(500) NOT NULL DEFAULT '',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (owner_email) REFERENCES users(email) ON DELETE CASCADE
);

-- 3. Demo accounts
INSERT IGNORE INTO users (email, password_hash, role, department) VALUES
('resident@example.test', 'resident123', 'RESIDENT', ''),
('worker@example.test', 'worker123', 'WORKER', 'Roads'),
('sidewalkworker@example.test', 'worker123', 'WORKER', 'Sidewalks'),
('supervisor@example.test', 'supervisor123', 'SUPERVISOR', '');

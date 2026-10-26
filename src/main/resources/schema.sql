-- Ottawa 311 Infrastructure Reporting System - MySQL DDL Schema
CREATE DATABASE IF NOT EXISTS ottawa311_db;
USE ottawa311_db;

DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS users;

-- 1. Users Table
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'RESIDENT', 'WORKER', 'SUPERVISOR'
    department VARCHAR(50) DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Reports Table
CREATE TABLE reports (
    report_id VARCHAR(30) PRIMARY KEY, -- e.g., 'OTT-1001'
    owner_email VARCHAR(254) NOT NULL,
    location VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    urgency VARCHAR(20) NOT NULL,
    photo_name VARCHAR(100) DEFAULT '',
    department VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED',
    notes VARCHAR(500) DEFAULT '',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (owner_email) REFERENCES users(email) ON DELETE CASCADE
);

-- 3. Seed Data (Pre-populating default demo accounts)
INSERT INTO users (email, password_hash, role, department) VALUES
('resident@example.test', 'resident123', 'RESIDENT', ''),
('worker@example.test', 'worker123', 'WORKER', 'Roads'),
('sidewalkworker@example.test', 'worker123', 'WORKER', 'Sidewalks'),
('supervisor@example.test', 'supervisor123', 'SUPERVISOR', '');
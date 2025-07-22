-- Создать базу (если ещё нет)
CREATE DATABASE IF NOT EXISTS kyc_control DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kyc_control;

-- Таблица устройств
CREATE TABLE devices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(64) UNIQUE,
    brand VARCHAR(64),
    model VARCHAR(64),
    android_version VARCHAR(32),
    registered_at DATETIME,
    last_seen DATETIME DEFAULT NULL
);

-- Таблица команд
CREATE TABLE commands (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(64),
    type ENUM('open', 'close', 'interval'),
    payload TEXT,
    issued_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Таблица журнала
CREATE TABLE logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(64),
    command_type VARCHAR(16),
    payload TEXT,
    executed_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

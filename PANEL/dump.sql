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
    type ENUM('open', 'close', 'interval', 'image_report', 'tesseract'),
    payload TEXT,
    issued_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Таблица журнала
CREATE TABLE logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(64),
    command_type VARCHAR(32),
    payload TEXT,
    executed_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Таблица для отчётов об изображениях
CREATE TABLE journal (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(64),
    event_type VARCHAR(32),
    payload LONGTEXT,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Таблицы для анализа изображений (создаются автоматически в image_analysis.php)
-- Пример структуры:
-- CREATE TABLE analysis_{device_id} (
--     id INT AUTO_INCREMENT PRIMARY KEY,
--     timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     image_path TEXT,
--     image_text LONGTEXT,
--     is_safe BOOLEAN
-- );

<?php

// Подключение к базе данных из файла db.php
require_once '../db.php';

// Получаем device_id из GET-запроса
if (!isset($_GET['device_id'])) {
    http_response_code(400);
    exit('Missing device_id');
}

$device_id = $_GET['device_id'];

// Получаем JSON-данные из тела POST-запроса
$data = json_decode(file_get_contents('php://input'), true);

if (!$data) {
    http_response_code(400);
    exit('Invalid JSON');
}

// Подготовка и запись данных в журнал
$stmt = $pdo->prepare("INSERT INTO journal (device_id, event_type, payload, timestamp) VALUES (?, 'image_report', ?, NOW())");

try {
    $stmt->execute([
        $device_id,
        json_encode($data, JSON_UNESCAPED_UNICODE)
    ]);
} catch (PDOException $e) {
    http_response_code(500);
    exit('Failed to save report');
}

http_response_code(200);
echo 'Report saved successfully';

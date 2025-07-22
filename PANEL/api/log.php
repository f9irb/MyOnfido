<?php
require '../db.php';

$data = json_decode(file_get_contents('php://input'), true);

$device_id = $data['deviceId'] ?? '';
$type      = $data['type'] ?? '';
$payload   = $data['payload'] ?? '';
$silent    = $data['silent'] ?? false;

if (!$device_id || !$type) {
    http_response_code(400);
    echo json_encode(['error' => 'Missing fields']);
    exit;
}

// Всегда обновляем last_seen устройства
$stmtUpdate = $pdo->prepare("UPDATE devices SET last_seen = NOW() WHERE device_id = ?");
$stmtUpdate->execute([$device_id]);

// Если silent-запрос, не пишем в журнал
if ($silent) {
    echo json_encode(['status' => 'silent update']);
    exit;
}

// Обычное логирование
$stmt = $pdo->prepare("INSERT INTO logs (device_id, command_type, payload) VALUES (?, ?, ?)");
$stmt->execute([$device_id, $type, $payload]);

echo json_encode(['status' => 'logged']);

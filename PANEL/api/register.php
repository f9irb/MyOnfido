<?php
require '../db.php';

$data = json_decode(file_get_contents('php://input'), true);

$device_id = $data['deviceId'] ?? '';
$model     = $data['model'] ?? '';
$brand     = $data['brand'] ?? '';
$android   = $data['android'] ?? '';
$time      = date('Y-m-d H:i:s', intval($data['time']) / 1000);

if ($device_id) {
    $stmt = $pdo->prepare("INSERT IGNORE INTO devices (device_id, model, brand, android_version, registered_at) VALUES (?, ?, ?, ?, ?)");
    $stmt->execute([$device_id, $model, $brand, $android, $time]);
    echo "OK";
} else {
    http_response_code(400);
    echo "Missing device_id";
}
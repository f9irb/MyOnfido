<?php
require '../db.php';

$device_id = $_GET['deviceId'] ?? '';
if (!$device_id) {
    http_response_code(400);
    echo json_encode(['error' => 'Missing deviceId']);
    exit;
}

// Получаем первую команду
$stmt = $pdo->prepare("SELECT * FROM commands WHERE device_id = ? ORDER BY issued_at ASC LIMIT 1");
$stmt->execute([$device_id]);
$cmd = $stmt->fetch();

if ($cmd) {
    echo json_encode([
        'id' => $cmd['id'],
        'type' => $cmd['type'],
        'payload' => $cmd['payload']
    ]);

    // Удаляем команду сразу после выдачи
    $del = $pdo->prepare("DELETE FROM commands WHERE id = ?");
    $del->execute([$cmd['id']]);

} else {
    echo json_encode(['type' => 'none']);
}

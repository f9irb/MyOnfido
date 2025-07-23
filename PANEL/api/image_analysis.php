<?php

require_once '../db.php';

$data = json_decode(file_get_contents('php://input'), true);

if (!$data || !isset($data['device_id'], $data['image_path'], $data['image_text'], $data['is_safe'])) {
    http_response_code(400);
    exit('Invalid JSON data');
}

$device_id = $data['device_id'];
$image_path = $data['image_path'];
$image_text = $data['image_text'];
$is_safe = $data['is_safe'] ? 1 : 0;

$table_name = "analysis_" . preg_replace('/[^a-zA-Z0-9_]/', '_', $device_id);

try {
    $pdo->exec("CREATE TABLE IF NOT EXISTS `$table_name` (
        id INT AUTO_INCREMENT PRIMARY KEY,
        timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        image_path TEXT,
        image_text LONGTEXT,
        is_safe BOOLEAN
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

    $stmt = $pdo->prepare("SELECT COUNT(*) FROM `$table_name` WHERE image_path = ?");
    $stmt->execute([$image_path]);

    if ($stmt->fetchColumn() == 0) {
        $insert = $pdo->prepare("INSERT INTO `$table_name` (image_path, image_text, is_safe) VALUES (?, ?, ?)");
        $insert->execute([$image_path, $image_text, $is_safe]);
    }
} catch (PDOException $e) {
    http_response_code(500);
    exit('Database error');
}

http_response_code(200);
echo 'Analysis result saved successfully';

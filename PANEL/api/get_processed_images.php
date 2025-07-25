<?php

require_once '../db.php';

if (!isset($_GET['device_id'])) {
    http_response_code(400);
    exit('Missing device_id');
}

$device_id = $_GET['device_id'];
$table_name = "analysis_" . preg_replace('/[^a-zA-Z0-9_]/', '_', $device_id);

try {
    $stmt = $pdo->query("SELECT image_path FROM `$table_name`");
    $paths = $stmt->fetchAll(PDO::FETCH_COLUMN);

    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($paths, JSON_UNESCAPED_UNICODE);

} catch (PDOException $e) {
    http_response_code(500);
    exit(json_encode([]));
}

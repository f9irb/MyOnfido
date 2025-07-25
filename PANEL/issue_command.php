<?php
require 'db.php';

$device_id = $_POST['device_id'] ?? '';
$type = $_POST['type'] ?? '';
$payload = $_POST['payload'] ?? '';

$allowed_commands = ['open', 'close', 'interval', 'image_report', 'tesseract'];

if ($device_id && in_array($type, $allowed_commands)) {
    $stmt = $pdo->prepare("INSERT INTO commands (device_id, type, payload) VALUES (?, ?, ?)");
    $stmt->execute([$device_id, $type, $payload]);
}

header("Location: panel_fZrGxk71.php");
exit;

<?php
session_start();
if (!isset($_SESSION['auth'])) {
    header('Location: login_Lf6a3p.php');
    exit;
}
require 'db.php';
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Device Panel</title>
  <style>
    body { font-family: sans-serif; padding: 20px; }
    table { border-collapse: collapse; width: 100%; margin-bottom: 30px; }
    th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }
    th { background-color: #f0f0f0; }
    .section { margin-bottom: 40px; }
  </style>
</head>
<body>

<h1>📱 Устройства</h1>

<div class="section">
  <table>
    <tr>
      <th>ID</th>
      <th>Device ID</th>
      <th>Бренд</th>
      <th>Модель</th>
      <th>Android</th>
      <th>Регистрация</th>
      <th>Последнее обращение</th>
      <th>Команда</th>
    </tr>
    <?php
    $devices = $pdo->query("SELECT * FROM devices ORDER BY registered_at DESC")->fetchAll();
    foreach ($devices as $d):
    ?>
    <tr>
      <td><?= $d['id'] ?></td>
      <td><?= htmlspecialchars($d['device_id']) ?></td>
      <td><?= htmlspecialchars($d['brand']) ?></td>
      <td><?= htmlspecialchars($d['model']) ?></td>
      <td><?= htmlspecialchars($d['android_version']) ?></td>
      <td><?= $d['registered_at'] ?></td>
      <td><?= $d['last_seen'] ?? '—' ?></td>
      <td>
        <form action="issue_command.php" method="post" style="display:inline-block;">
          <input type="hidden" name="device_id" value="<?= htmlspecialchars($d['device_id']) ?>">
          <select name="type">
            <option value="open">open</option>
            <option value="close">close</option>
            <option value="interval">interval</option>
          </select>
          <input type="text" name="payload" placeholder="url или interval (сек.)" style="width: 200px;">
          <button type="submit">Отправить</button>
        </form>
      </td>
    </tr>
    <?php endforeach; ?>
  </table>
</div>

<h2>🕘 Журнал</h2>

<div class="section">
  <table>
    <tr>
      <th>Device ID</th><th>Тип</th><th>Payload</th><th>Выполнено</th>
    </tr>
    <?php
    $logs = $pdo->query("SELECT * FROM logs ORDER BY executed_at DESC LIMIT 50")->fetchAll();
    foreach ($logs as $log):
    ?>
    <tr>
      <td><?= htmlspecialchars($log['device_id']) ?></td>
      <td><?= htmlspecialchars($log['command_type']) ?></td>
      <td><?= htmlspecialchars($log['payload']) ?></td>
      <td><?= $log['executed_at'] ?></td>
    </tr>
    <?php endforeach; ?>
  </table>
</div>

</body>
</html>

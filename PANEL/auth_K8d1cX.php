<?php
session_start();

$user = 'admin';
$pass = '96n02lMSdXMge!'; // Установи свой!

if ($_POST['username'] === $user && $_POST['password'] === $pass) {
    $_SESSION['auth'] = true;
    header('Location: panel_fZrGxk71.php');
    exit;
} else {
    echo "❌ Неверный логин или пароль.";
}
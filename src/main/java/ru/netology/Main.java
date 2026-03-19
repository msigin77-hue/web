package ru.netology;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {

        Server server = new Server();

        // Запускаем сервер в отдельном потоке
        new Thread(() -> {
            server.start();
        }).start();

        // Даем серверу время запуститься
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Server is running. Press Enter to stop...");

        // Ожидаем ввод пользователя для остановки
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Останавливаем сервер
        server.stop();
        System.out.println("Server stopped.");
    }

}
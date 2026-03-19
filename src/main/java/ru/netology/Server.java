package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 9999;
    private static final int THREAD_POOL_SIZE = 64;
    private static final List<String> VALID_PATHS = List.of(
            "/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html",
            "/classic.html", "/events.html", "/events.js"
    );

    private final ExecutorService threadPool;
    private volatile boolean running;

    public Server() {
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.running = true;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Обрабатываем каждое подключение в отдельном потоке из пула
                    threadPool.submit(() -> handleConnection(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    public void stop() {
        running = false;
    }

    private void shutdown() {
        threadPool.shutdown();
        System.out.println("Server stopped");
    }

    private void handleConnection(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())) {

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

            try {
                Request request = new Request(requestLine);
                System.out.println("Processing: " + request); // Для отладки
                processRequest(request, out);
            } catch (IllegalArgumentException e) {
                sendErrorResponse(out, 400, "Bad Request: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
        }
    }

    private void processRequest(Request request, BufferedOutputStream out) throws IOException {
        // Поддерживаем только GET запросы
        if (!"GET".equals(request.getMethod())) {
            sendErrorResponse(out, 405, "Method Not Allowed");
            return;
        }

        String path = request.getPath();

        // Проверяем валидность пути
        if (!VALID_PATHS.contains(path)) {
            sendErrorResponse(out, 404, "Not Found");
            return;
        }

        // Обработка динамических эндпоинтов
        if (path.equals("/greeting")) {
            handleGreetingEndpoint(request, out);
            return;
        }

        // Обработка статических файлов
        Path filePath = Path.of(".", "public", path);
        if (!Files.exists(filePath)) {
            sendErrorResponse(out, 404, "Not Found");
            return;
        }

        sendFileResponse(out, filePath, path);
    }
    private void handleGreetingEndpoint(Request request, BufferedOutputStream out) throws IOException {
        // Получаем параметры из query string
        String name = request.getQueryParam("name");
        String language = request.getQueryParam("lang");

        if (name == null) {
            name = "Guest";
        }

        String greeting;
        if ("ru".equals(language)) {
            greeting = "Привет, " + name + "!";
        } else if ("es".equals(language)) {
            greeting = "¡Hola, " + name + "!";
        } else {
            greeting = "Hello, " + name + "!";
        }

        // Получаем все параметры для демонстрации
        Map<String, String> allParams = request.getQueryParams();

        // Формируем HTML ответ
        String html = String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head><title>Greeting</title></head>" +
                        "<body>" +
                        "<h1>%s</h1>" +
                        "<h2>All query parameters:</h2>" +
                        "<ul>%s</ul>" +
                        "<p><a href='/index.html'>Back to home</a></p>" +
                        "</body>" +
                        "</html>",
                greeting,
                formatParamsAsHtml(allParams)
        );

        byte[] contentBytes = html.getBytes();
        writeResponseHeaders(out, 200, "text/html", contentBytes.length);
        out.write(contentBytes);
        out.flush();
    }

    private String formatParamsAsHtml(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            sb.append(String.format("<li><b>%s</b>: %s</li>",
                    entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }

    private void sendFileResponse(BufferedOutputStream out, Path filePath, String originalPath) throws IOException {
        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        // Особая обработка для classic.html
        if (originalPath.equals("/classic.html")) {
            String template = Files.readString(filePath);
            String content = template.replace("{time}", LocalDateTime.now().toString());
            byte[] contentBytes = content.getBytes();

            writeResponseHeaders(out, 200, mimeType, contentBytes.length);
            out.write(contentBytes);
            out.flush();
            return;
        }

        // Обычный файл
        long fileSize = Files.size(filePath);
        writeResponseHeaders(out, 200, mimeType, fileSize);
        Files.copy(filePath, out);
        out.flush();
    }

    private void sendErrorResponse(BufferedOutputStream out, int statusCode, String statusText) throws IOException {
        String response = String.format(
                "HTTP/1.1 %d %s\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n",
                statusCode, statusText
        );
        out.write(response.getBytes());
        out.flush();
    }

    private void writeResponseHeaders(BufferedOutputStream out, int statusCode, String contentType, long contentLength) throws IOException {
        String headers = String.format(
                "HTTP/1.1 %d OK\r\n" +
                        "Content-Type: %s\r\n" +
                        "Content-Length: %d\r\n" +
                        "Connection: close\r\n" +
                        "\r\n",
                statusCode, contentType, contentLength
        );
        out.write(headers.getBytes());
    }

 }
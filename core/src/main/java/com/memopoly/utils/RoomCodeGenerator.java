package com.memopoly.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Генератор кодов комнат через Cloudflare Worker:
 * - Хост: POST {ip, port} → получает короткий код (например "X7K9PQ")
 * - Гость: GET /room/:code → получает {ip, port}
 */
public class RoomCodeGenerator {
    private static final String WORKER_URL = "https://memopoly.egordatcenko7.workers.dev";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    /**
     * Создаёт комнату: отправляет IP+порт на Worker, получает короткий код.
     * @param ip IP-адрес хоста
     * @param port порт сервера
     * @return короткий код комнаты (например "X7K9PQ") или null при ошибке
     */
    public static String createRoomCode(String ip, int port) {
        try {
            String json = String.format("{\"ip\":\"%s\",\"port\":%d}", ip, port);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WORKER_URL + "/room"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                AppLog.warn("RoomCode", "Worker вернул статус " + response.statusCode() + ": " + response.body());
                return null;
            }

            String body = response.body();
            Matcher matcher = Pattern.compile("\"code\":\"([^\"]+)\"").matcher(body);
            if (matcher.find()) {
                String code = matcher.group(1);
                AppLog.info("RoomCode", "Создан код комнаты: " + code + " для " + ip + ":" + port);
                return code;
            } else {
                AppLog.warn("RoomCode", "Не удалось извлечь код из ответа: " + body);
                return null;
            }
        } catch (Exception e) {
            AppLog.warn("RoomCode", "Ошибка создания кода: " + e.getMessage());
            return null;
        }
    }

    /**
     * Расшифровывает код комнаты: запрашивает IP+порт у Worker.
     * @param code короткий код комнаты
     * @return RoomInfo с IP и портом или null при ошибке/истечении срока
     */
    public static RoomInfo decodeRoomCode(String code) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WORKER_URL + "/room/" + code))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                AppLog.warn("RoomCode", "Worker вернул статус " + response.statusCode() + " для кода " + code);
                return null;
            }

            String body = response.body();
            Matcher ipMatcher = Pattern.compile("\"ip\":\"([^\"]+)\"").matcher(body);
            Matcher portMatcher = Pattern.compile("\"port\":(\\d+)").matcher(body);

            if (ipMatcher.find() && portMatcher.find()) {
                String ip = ipMatcher.group(1);
                int port = Integer.parseInt(portMatcher.group(1));
                AppLog.info("RoomCode", "Расшифрован код " + code + " → " + ip + ":" + port);
                return new RoomInfo(ip, port);
            } else {
                AppLog.warn("RoomCode", "Не удалось извлечь IP/port из ответа: " + body);
                return null;
            }
        } catch (Exception e) {
            AppLog.warn("RoomCode", "Ошибка расшифровки кода " + code + ": " + e.getMessage());
            return null;
        }
    }

    public static class RoomInfo {
        public final String ip;
        public final int port;

        public RoomInfo(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }
}

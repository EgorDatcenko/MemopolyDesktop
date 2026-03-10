package com.memopoly.utils;

public class RoomCodeGenerator {

    private static final String[] FIXED_WORDS = {
        "PEPE", "BIGBOB", "GLIST", "SKIBIDI", "CHUPEP", "SKEBOB"
    };

    public static String encodeIP(String ip) {
        String[] parts = ip.split("\\.");
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < Math.min(parts.length, 4); i++) {
            try {
                int number = Integer.parseInt(parts[i]);
                String base36 = Integer.toString(number, 36).toUpperCase();
                String word = FIXED_WORDS[i % FIXED_WORDS.length];

                // Явный разделитель между числом и словом
                code.append(base36).append("_").append(word);

                if (i < 3) code.append("-");
            } catch (NumberFormatException e) {
                code.append("0_RED-");
            }
        }

        code.append("-").append(generateSimpleChecksum(ip));
        return code.toString();
    }

    public static String decodeRoomCode(String roomCode) {
        try {
            String[] parts = roomCode.split("-");

            if (parts.length < 4) {
                return "127.0.0.1";
            }

            StringBuilder ip = new StringBuilder();

            for (int i = 0; i < 4; i++) {
                String part = parts[i];

                // Разделяем по "_" чтобы отделить base36 от слова
                String[] numberAndWord = part.split("_");

                if (numberAndWord.length < 2) {
                    return "127.0.0.1";
                }

                try {
                    String base36Str = numberAndWord[0];
                    int number = Integer.parseInt(base36Str, 36);

                    if (number < 0 || number > 255) {
                        return "127.0.0.1";
                    }

                    ip.append(number);
                    if (i < 3) ip.append(".");
                } catch (NumberFormatException e) {
                    return "127.0.0.1";
                }
            }

            // Проверяем контрольную сумму
            if (parts.length > 4) {
                String providedChecksum = parts[4];
                String calculatedChecksum = generateSimpleChecksum(ip.toString());

                if (!providedChecksum.equals(calculatedChecksum)) {
                    System.out.println("Неверная контрольная сумма!");
                    System.out.println("IP: " + ip);
                    System.out.println("Ожидалось: " + calculatedChecksum + ", получено: " + providedChecksum);
                    return "127.0.0.1";
                }
            }

            return ip.toString();

        } catch (Exception e) {
            System.out.println("Ошибка декодирования: " + e.getMessage());
            return "127.0.0.1";
        }
    }

    private static String generateSimpleChecksum(String ip) {
        int sum = 0;
        String[] parts = ip.split("\\.");

        for (int i = 0; i < parts.length; i++) {
            try {
                int number = Integer.parseInt(parts[i]);
                sum += number * (i + 1);
            } catch (NumberFormatException e) {
                sum += 127 * (i + 1);
            }
        }

        return Integer.toString(sum % 10000);
    }

    public static void main(String[] args) {
        String ip = "251.168.1.101";
        String code = encodeIP(ip);
        String decoded = decodeRoomCode(code);

        System.out.println("IP: " + ip);
        System.out.println("Code: " + code);
        System.out.println("Decoded: " + decoded);
        System.out.println("Correct: " + ip.equals(decoded));

        // Тест с другими IP
        System.out.println("\nДополнительные тесты:");
        testIP("127.0.0.1");
        testIP("10.0.0.1");
        testIP("255.255.255.255");
    }

    private static void testIP(String ip) {
        String code = encodeIP(ip);
        String decoded = decodeRoomCode(code);
        System.out.println(ip + " -> " + code + " -> " + decoded + " [" + ip.equals(decoded) + "]");
    }
}

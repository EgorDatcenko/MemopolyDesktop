package com.memopoly.network.twitch;

import com.badlogic.gdx.Gdx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Легковесный клиентский сервис подключения к Twitch Chat (IRC)
 * через нативный TCP Socket без использования API-ключей и сторонних серверов.
 */
public class TwitchChatService {

    private static final String IRC_HOST = "irc.chat.twitch.tv";
    private static final int IRC_PORT = 6667;
    private static final int CONNECT_TIMEOUT_MS = 3000;

    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    public interface StatusListener {
        void onStatusChanged(ConnectionStatus status);
    }

    public interface VoteListener {
        void onVotesUpdated();
    }

    private volatile ConnectionStatus status = ConnectionStatus.DISCONNECTED;
    private volatile String currentChannel = "";

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Thread readerThread;
    private final AtomicBoolean isDisconnecting = new AtomicBoolean(false);

    private StatusListener statusListener;
    private VoteListener voteListener;

    // Голоса зрителей: никнейм зрителя -> номер выбранного слота мема (1..4)
    private final Map<String, Integer> chatVotes = new ConcurrentHashMap<>();

    public void setStatusListener(StatusListener listener) {
        this.statusListener = listener;
        if (listener != null) {
            listener.onStatusChanged(this.status);
        }
    }

    public void setVoteListener(VoteListener listener) {
        this.voteListener = listener;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public boolean isConnected() {
        return status == ConnectionStatus.CONNECTED;
    }

    public String getCurrentChannel() {
        return currentChannel;
    }

    private void updateStatus(ConnectionStatus newStatus) {
        this.status = newStatus;
        if (statusListener != null) {
            try {
                statusListener.onStatusChanged(newStatus);
            } catch (Exception e) {
                Gdx.app.error("TwitchChatService", "Error in status listener", e);
            }
        }
    }

    /**
     * Асинхронное подключение к указанному каналу Twitch.
     */
    public synchronized void connectAsync(String channelName) {
        if (channelName == null || channelName.trim().isEmpty()) {
            disconnect();
            return;
        }

        final String normalizedChannel = channelName.trim().toLowerCase().replace("@", "");
        if (normalizedChannel.isEmpty()) {
            disconnect();
            return;
        }

        // Если уже подключены к этому же каналу, повторно не переподключаемся
        if (status == ConnectionStatus.CONNECTED && normalizedChannel.equals(currentChannel)) {
            return;
        }

        disconnect();

        this.currentChannel = normalizedChannel;
        isDisconnecting.set(false);
        updateStatus(ConnectionStatus.CONNECTING);

        Thread connectThread = new Thread(() -> {
            try {
                Socket sock = new Socket();
                sock.connect(new InetSocketAddress(IRC_HOST, IRC_PORT), CONNECT_TIMEOUT_MS);

                BufferedReader r = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter w = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));

                synchronized (TwitchChatService.this) {
                    if (isDisconnecting.get()) {
                        try { sock.close(); } catch (Exception ignored) {}
                        return;
                    }
                    socket = sock;
                    reader = r;
                    writer = w;
                }

                // Анонимная авторизация JustinFan
                String nick = "justinfan" + (10000 + new Random().nextInt(90000));
                w.write("PASS oauth:empty\r\n");
                w.write("NICK " + nick + "\r\n");
                w.write("USER " + nick + " 8 * :" + nick + "\r\n");
                w.write("JOIN #" + normalizedChannel + "\r\n");
                w.flush();

                startReaderThread();

            } catch (Exception e) {
                if (!isDisconnecting.get()) {
                    Gdx.app.error("TwitchChatService", "Connection failed for #" + normalizedChannel + ": " + e.getMessage());
                    cleanupResources();
                    updateStatus(ConnectionStatus.ERROR);
                }
            }
        }, "TwitchChat-Connect");

        connectThread.setDaemon(true);
        connectThread.start();
    }

    private void startReaderThread() {
        readerThread = new Thread(() -> {
            try {
                String line;
                while (!isDisconnecting.get() && (line = reader.readLine()) != null) {
                    handleIrcLine(line);
                }
            } catch (SocketException e) {
                // Штатное закрытие соединения при disconnect()
            } catch (Exception e) {
                if (!isDisconnecting.get() && !Thread.currentThread().isInterrupted()) {
                    Gdx.app.error("TwitchChatService", "IRC read error: " + e.getMessage());
                    cleanupResources();
                    updateStatus(ConnectionStatus.ERROR);
                    return;
                }
            }

            if (!isDisconnecting.get()) {
                cleanupResources();
                updateStatus(ConnectionStatus.DISCONNECTED);
            }
        }, "TwitchChat-Reader");

        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void handleIrcLine(String line) {
        if (line == null) return;

        // Обработка серверного PING
        if (line.startsWith("PING")) {
            sendRaw("PONG :tmi.twitch.tv\r\n");
            return;
        }

        // Подтверждение подключения (welcome message или успешный JOIN)
        if (status == ConnectionStatus.CONNECTING) {
            if (line.contains(":tmi.twitch.tv 001") || line.contains("JOIN #" + currentChannel)) {
                updateStatus(ConnectionStatus.CONNECTED);
                Gdx.app.log("TwitchChatService", "Successfully connected to #" + currentChannel);
            } else if (line.contains(":tmi.twitch.tv NOTICE * :Login authentication failed")
                    || line.contains(":tmi.twitch.tv NOTICE * :Error")) {
                updateStatus(ConnectionStatus.ERROR);
            }
        }

        // Парсинг сообщений чата PRIVMSG
        if (line.contains("PRIVMSG #" + currentChannel)) {
            parsePrivMsg(line);
        }
    }

    private void parsePrivMsg(String line) {
        int exclIndex = line.indexOf('!');
        int colonIndex = line.indexOf(" :", 1);
        if (exclIndex > 1 && colonIndex > exclIndex) {
            String sender = line.substring(1, exclIndex).toLowerCase();
            String message = line.substring(colonIndex + 2).trim();

            if ("1".equals(message) || "2".equals(message) || "3".equals(message) || "4".equals(message)) {
                int slot = Integer.parseInt(message);
                chatVotes.put(sender, slot);
                if (voteListener != null) {
                    try {
                        voteListener.onVotesUpdated();
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private synchronized void sendRaw(String raw) {
        try {
            if (writer != null) {
                writer.write(raw);
                writer.flush();
            }
        } catch (Exception ignored) {}
    }

    /**
     * Безопасное отключение сокета и потоков без утечек ресурсов.
     */
    public synchronized void disconnect() {
        isDisconnecting.set(true);
        cleanupResources();
        updateStatus(ConnectionStatus.DISCONNECTED);
    }

    private synchronized void cleanupResources() {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {}
            socket = null;
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (Exception ignored) {}
            reader = null;
        }
        if (writer != null) {
            try {
                writer.close();
            } catch (Exception ignored) {}
            writer = null;
        }
        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }
    }

    /**
     * Очистить голоса перед началом голосования.
     */
    public void startVotingSession() {
        chatVotes.clear();
        if (voteListener != null) {
            try {
                voteListener.onVotesUpdated();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Получить количество зрительских голосов за конкретный слот (1..4).
     */
    public int getVotesForSlot(int slot) {
        int count = 0;
        for (int votedSlot : chatVotes.values()) {
            if (votedSlot == slot) {
                count++;
            }
        }
        return count;
    }

    /**
     * Вычислить слот-победитель чата Твича.
     * Возвращает 1..maxSlots если есть единственный слот с абсолютным большинством голосов.
     * Возвращает 0, если голосов нет или при ничьей за 1 место.
     */
    public int getMajoritySlot(int maxSlots) {
        if (maxSlots <= 0 || chatVotes.isEmpty()) {
            return 0;
        }

        int[] counts = new int[maxSlots + 1];
        for (int slot : chatVotes.values()) {
            if (slot >= 1 && slot <= maxSlots) {
                counts[slot]++;
            }
        }

        int maxVotes = 0;
        int bestSlot = 0;
        boolean tie = false;

        for (int s = 1; s <= maxSlots; s++) {
            if (counts[s] > maxVotes) {
                maxVotes = counts[s];
                bestSlot = s;
                tie = false;
            } else if (counts[s] == maxVotes && maxVotes > 0) {
                tie = true;
            }
        }

        if (tie || maxVotes == 0) {
            return 0;
        }
        return bestSlot;
    }

    public Map<Integer, Integer> getAllSlotVotes(int maxSlots) {
        Map<Integer, Integer> result = new ConcurrentHashMap<>();
        for (int s = 1; s <= maxSlots; s++) {
            result.put(s, getVotesForSlot(s));
        }
        return result;
    }

    /**
     * Подпись текущего состояния голосов для реактивного обновления UI.
     */
    public String getVotesSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(chatVotes.size()).append(':');
        for (int i = 1; i <= 4; i++) {
            sb.append(getVotesForSlot(i)).append(',');
        }
        return sb.toString();
    }
}

package com.memopoly.network.packets;

/**
 * Пакет текстового чата: содержит данные текстового сообщения для отображения в игровом чате.
 */
public class ChatMessage {
    public int playerId;
    public String playerName;
    public String message;
    public boolean isSystem;
    public long timestamp;
}

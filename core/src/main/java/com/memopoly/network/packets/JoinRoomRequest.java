package com.memopoly.network.packets;

/**
 * Пакет запроса входа в комнату: клиент отправляет код комнаты для подключения к игровой сессии.
 */
public class JoinRoomRequest {
    public String playerName;
    public String hostIP;
    public int port;
    public JoinRoomRequest() {}
}

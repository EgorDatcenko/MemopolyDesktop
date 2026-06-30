package com.memopoly.network.packets;

/**
 * Пакет запроса создания комнаты: отправляется хостом для инициализации новой игровой сессии на сервере.
 */
public class CreateRoomRequest {
    public int maxPlayers;
    public String roomName;
    public String hostName;
}

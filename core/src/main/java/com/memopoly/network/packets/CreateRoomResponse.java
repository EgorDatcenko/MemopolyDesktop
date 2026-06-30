package com.memopoly.network.packets;

/**
 * Пакет ответа на создание комнаты: подтверждает создание комнаты и содержит сгенерированный код комнаты.
 */
public class CreateRoomResponse {
    public String roomCode;
    public String hostIP;
    public int port;
    public boolean success;
}

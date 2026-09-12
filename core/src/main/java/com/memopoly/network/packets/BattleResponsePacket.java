package com.memopoly.network.packets;

/**
 * Пакет ответа на приглашение: клиент сообщает серверу о согласии или отказе участвовать в мем-баттле.
 */
public class BattleResponsePacket {
    public int playerId;
    public boolean accepted;
    public BattleResponsePacket() {}
}

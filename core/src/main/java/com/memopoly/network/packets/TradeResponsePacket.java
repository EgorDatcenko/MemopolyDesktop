package com.memopoly.network.packets;

/**
 * Пакет ответа на сделку: принимающий игрок подтверждает или отклоняет.
 */
public class TradeResponsePacket {
    public boolean accept;  // true = подтвердить, false = отклонить
}

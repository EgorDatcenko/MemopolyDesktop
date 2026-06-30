package com.memopoly.network.packets;

/**
 * Пакет результата броска кубиков: рассылается сервером всем игрокам с выпавшими значениями кубиков.
 */
public class RollDiceResponse {
    public int playerId;
    public int dice1;
    public int dice2;
    public int total;
    public int newPosition;
}

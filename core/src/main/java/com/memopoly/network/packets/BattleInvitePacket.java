package com.memopoly.network.packets;

import com.memopoly.game.model.GameState;

/**
 * Пакет приглашения в баттл: сервер рассылает игрокам предложение принять участие в мем-баттле.
 */
public class BattleInvitePacket {
    public int organizerId;
    public int cellIndex;
    public int stakes;
    public String topic;
    public GameState.BattleType battleType;
    public int timerSeconds;  
    public BattleInvitePacket() {}
}

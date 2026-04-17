package com.memopoly.network.packets;

import com.memopoly.game.model.GameState;

public class BattleInvitePacket {
    public int organizerId;
    public int cellIndex;
    public int stakes;
    public String topic;
    public GameState.BattleType battleType;
    public int timerSeconds;  // сколько секунд на ответ
    public BattleInvitePacket() {}
}

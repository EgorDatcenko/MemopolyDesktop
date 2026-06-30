package com.memopoly.network.packets;

/**
 * Пакет игрового действия: отправляется клиентом для выполнения какого-либо хода на сервере.
 */
public class GameActionRequest {
    public enum ActionType {
        BUY_CELL,
        PASS_BUY,
        MORTGAGE_CELL,
        BUY_BACK_CELL,
        START_MEME_BATTLE,
        SUBMIT_MEME,
        VOTE_MEME,
        MEME_BANK,
        MEME_BANK_DEPOSIT,
        MEME_BANK_WITHDRAW,
        MEME_BANK_SKIP,
        PLACE_AUCTION_BID,
        CANCEL_AUCTION,
        END_TURN
    }

    public ActionType actionType;
    public int targetId;
    public int amount;
    public String data;
}

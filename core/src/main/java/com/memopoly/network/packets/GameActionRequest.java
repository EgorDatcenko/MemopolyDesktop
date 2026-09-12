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
        BUY_HOUSE,
        SELL_HOUSE,
        START_MEME_BATTLE,
        SUBMIT_MEME,
        VOTE_MEME,
        MEME_BANK,
        MEME_BANK_DEPOSIT,
        MEME_BANK_WITHDRAW,
        MEME_BANK_SKIP,
        PLACE_AUCTION_BID,
        CANCEL_AUCTION,
        PAY_JAIL_FINE,
        END_TURN,
        CANCEL_MEME,
        CANCEL_MEME_BATTLE,
        STEAL_COINS,
        PLUS_TWO,
        CONFIRM_LANDING,
        MODERATOR_SKIP_JAIL,
        MODERATOR_TAKE_JAIL,
        DEV_SET_TEST_MODE,
        DEV_NEXT_GROUP_CELL,
        DEV_SET_MONEY,
        DEV_GRANT_MONOPOLY
    }

    public ActionType type;
    public int amount;
    public ActionType actionType;
    public int targetId;;
    public String data;
}

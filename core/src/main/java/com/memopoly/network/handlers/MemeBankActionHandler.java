package com.memopoly.network.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;
import com.memopoly.network.packets.GameActionRequest;

import java.util.function.IntPredicate;
import java.util.function.Supplier;

/**
 * Обработчик операций с банком мемов: обрабатывает взносы, снятие средств и обновление баланса игрока на клетке банка.
 */
public class MemeBankActionHandler implements GameActionHandler {
    private final GameState gameState;
    private final IntPredicate canUseMemeBank;
    private final Supplier<Player> currentPlayer;
    private final Runnable finishMemeBankAction;

    public MemeBankActionHandler(GameState gameState,
                                 IntPredicate canUseMemeBank,
                                 Supplier<Player> currentPlayer,
                                 Runnable finishMemeBankAction) {
        this.gameState = gameState;
        this.canUseMemeBank = canUseMemeBank;
        this.currentPlayer = currentPlayer;
        this.finishMemeBankAction = finishMemeBankAction;
    }

    @Override
    public boolean handle(Connection connection, Player actingPlayer, GameActionRequest request) {
        if (request.actionType != GameActionRequest.ActionType.MEME_BANK_DEPOSIT
            && request.actionType != GameActionRequest.ActionType.MEME_BANK_WITHDRAW
            && request.actionType != GameActionRequest.ActionType.MEME_BANK_SKIP) {
            return false;
        }

        if (!canUseMemeBank.test(connection.getID())) {
            return true;
        }

        Player current = currentPlayer.get();
        if (current == null) {
            return true;
        }

        switch (request.actionType) {
            case MEME_BANK_DEPOSIT:
                if (request.amount <= 0 || request.amount > 500) {
                    gameState.lastActionLog = current.name + " не может внести такую сумму в Meme Bank";
                    return true;
                }
                if (request.amount > current.money) {
                    gameState.lastActionLog = current.name + " не хватает наличных для вклада в Meme Bank";
                    return true;
                }
                current.money -= request.amount;
                current.memeBankBalance += request.amount;
                gameState.lastActionLog = current.name + " внёс " + request.amount + " в Meme Bank";
                finishMemeBankAction.run();
                return true;
            case MEME_BANK_WITHDRAW:
                if (current.memeBankBalance <= 0) {
                    gameState.lastActionLog = current.name + " попытался снять деньги из пустого Meme Bank";
                    return true;
                }
                int withdrawnAmount = current.memeBankBalance;
                current.money += withdrawnAmount;
                current.memeBankBalance = 0;
                gameState.lastActionLog = current.name + " снял " + withdrawnAmount + " из Meme Bank";
                finishMemeBankAction.run();
                return true;
            case MEME_BANK_SKIP:
                gameState.lastActionLog = current.name + " пропустил действие на Meme Bank";
                finishMemeBankAction.run();
                return true;
            default:
                return false;
        }
    }
}

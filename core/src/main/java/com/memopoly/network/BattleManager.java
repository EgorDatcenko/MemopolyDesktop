package com.memopoly.network;

import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier;
import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Meme;
import com.memopoly.game.model.Player;
import com.memopoly.network.packets.BattleResponsePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.memopoly.game.model.GameState.BattlePhase.*;
import static com.memopoly.game.model.GameState.BattleType.MEME_BATTLE_CELL;
import static com.memopoly.game.model.GameState.BattleType.SITUATION_CELL;

public class BattleManager {

    private GameState gameState;
    private List<BoardCell> boardCells;
    private Runnable onBroadcast;
    private int battleBank;
    private int battleOwnerId;
    private int battleCellIndex;
    public int battleTimerSeconds;
    private ArrayList<Integer> battleInvited;
    public BattleManager(GameState gameState, List<BoardCell> board, Runnable onBroadcast) {
        this.gameState = gameState;
        this.boardCells = board;
        this.onBroadcast = onBroadcast;
        this.battleInvited = new ArrayList<>();
    }
    public void startBattle(int organizerId, int cellIndex, int initialBank){
        gameState.currentPhase = GameState.GamePhase.MEME_BATTLE;
        gameState.isInBattle = true;
        battleOwnerId = organizerId;
        battleCellIndex = cellIndex;
        if (gameState.battleType == MEME_BATTLE_CELL) {
            battleBank = 0;

        } else if (gameState.battleType == SITUATION_CELL) {
            battleBank = initialBank;
        }

        battleInvited.clear();
        for (Player player : gameState.players) {
            if (player.id != organizerId) {
                battleInvited.add(player.id);
            }
        }
        gameState.battleOwnerId = organizerId;
        gameState.battlePhase = GameState.BattlePhase.INVITE;
        gameState.battleTimerSeconds = 30;
        gameState.battleBank = battleBank;
        gameState.battleInvited = battleInvited;
        gameState.battleParticipants.clear();
        gameState.battleParticipants.add(organizerId); // организатор участвует всегда
        gameState.battleMemes.clear();
        gameState.votes.clear();
        gameState.battleVoters.clear();

        onBroadcast.run();
    }

    public void handleBattleResponse(BattleResponsePacket packet){
        if (!battleInvited.contains(packet.playerId)) return;

        if (packet.accepted) {
            gameState.battleParticipants.add(packet.playerId);

            if (gameState.battleType == GameState.BattleType.MEME_BATTLE_CELL) {
                // найти игрока и списать ставку
                Player player = gameState.getPlayerById(packet.playerId);
                if (player != null) {
                    player.pay(gameState.battleStakes);
                    battleBank += gameState.battleStakes;
                    gameState.battleBank = battleBank;
                }
            }
        }

        battleInvited.remove(Integer.valueOf(packet.playerId));
        gameState.battleInvited = battleInvited;

        if (battleInvited.isEmpty()) {
            startCollecting();
        }

        onBroadcast.run();
    }
    public void startCollecting(){
        gameState.battlePhase = COLLECTING_MEMES;
        gameState.battleTimerSeconds = 60;
        Thread collectingThread = new Thread(() -> {
            // Этот код выполняется в отдельном потоке
            while (gameState.battleTimerSeconds > 0 && gameState.isInBattle) {
                try {
                    Thread.sleep(1000); // пауза 1 секунда
                } catch (InterruptedException e) {
                    return; // поток прерван — выходим
                }
                gameState.battleTimerSeconds--;
                if (gameState.battleMemes.size() >= gameState.battleParticipants.size()) {
                    gameState.battleTimerSeconds = 0;
                }
                onBroadcast.run();
            }
            startVoting();
        });

        collectingThread.setDaemon(true);
        collectingThread.start();
    }
    public void startVoting(){
        gameState.battlePhase = VOTING;
        gameState.battleTimerSeconds = 30;
        Thread voitingThread = new Thread(() -> {
            // Этот код выполняется в отдельном потоке
            while (gameState.battleTimerSeconds > 0 && gameState.isInBattle) {
                try {
                    Thread.sleep(1000); // пауза 1 секунда
                } catch (InterruptedException e) {
                    return; // поток прерван — выходим
                }
                gameState.battleTimerSeconds--;
                if (gameState.votes.size() >= gameState.battleParticipants.size()) {
                    gameState.battleTimerSeconds = 0;
                }
                onBroadcast.run();
            }
            countVotes();
        });
        voitingThread.setDaemon(true);
        voitingThread.start();
    }
    public void countVotes() {
        int max = 0;
        for (int votes : gameState.votes.values()) {
            if (votes > max) max = votes;
        }

        ArrayList<Integer> winners = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : gameState.votes.entrySet()) {
            if (entry.getValue() == max) {
                winners.add(entry.getKey()); // memeId
            }
        }

        if (max == 0 || winners.size() == gameState.battleMemes.size()) {
            handleDraw();
        } else if (winners.size() == 1) {
            handleWinner(winners.get(0));

        } else {
            gameState.battleMemes.removeIf(meme -> !winners.contains(meme.id));
            gameState.votes.clear();
        gameState.battleVoters.clear();
            startVoting();
        }
    }
    public void handleWinner(int memeId){
        Meme winnerMeme = null;
        for (Meme meme : gameState.battleMemes) {
            if (meme.id == memeId) {
                winnerMeme = meme;
                break;
            }
        }
        Player winner = gameState.getPlayerById(winnerMeme.ownerId);
        for (Player player : gameState.players) {
            if (player.id == winner.id) continue; // победитель получает банк, не бонус
            for (Meme meme : gameState.battleMemes) {
                if (meme.ownerId == player.id) {
                    int votesForMeme = gameState.votes.getOrDefault(meme.id, 0);
                    player.receive(votesForMeme * 10);
                }
            }
        }
        int ownerCellId = gameState.cellOwners.getOrDefault(battleCellIndex, -1);
        Player ownerCell = gameState.getPlayerById(ownerCellId);
        if (gameState.battleType == MEME_BATTLE_CELL) {
            winner.money += battleBank;
        } else if (gameState.battleType == SITUATION_CELL) {
            winner.money += battleBank/2;
            ownerCell.money +=  battleBank/2;
        }
        gameState.battlePhase = RESULTS;
        onBroadcast.run();
        gameState.endMemeBattle();
        gameState.nextPlayer();
        onBroadcast.run();
    }
    public void handleDraw(){
        for (Player player : gameState.players) {
            for (Meme meme : gameState.battleMemes) {
                if (meme.ownerId == player.id) {
                    int votesForMeme = gameState.votes.getOrDefault(meme.id, 0);
                    player.receive(votesForMeme * 10);
                }
            }
        }
        int ownerCellId = gameState.cellOwners.getOrDefault(battleCellIndex, -1);
        Player ownerCell = gameState.getPlayerById(ownerCellId);
        if (gameState.battleType == MEME_BATTLE_CELL) {
            for (Player player : gameState.players) {
                for (Meme meme : gameState.battleMemes) {
                    if (meme.ownerId == player.id) {
                        player.money += battleBank / gameState.battleParticipants.size();
                    }
                }
            }
        }else if (gameState.battleType == SITUATION_CELL) {
            ownerCell.money += battleBank / 2;
            int rest = battleBank / 2;
            int othersCount = gameState.battleParticipants.size() - 1; // без владельца
            if (othersCount > 0) {
                int share = rest / othersCount;
                for (Player player : gameState.players) {
                    if (player.id == ownerCell.id) continue;
                    if (gameState.battleParticipants.contains(player.id)) {
                        player.money += share;
                    }
                }
            }
        }
        gameState.battlePhase = RESULTS;
        onBroadcast.run();
        gameState.endMemeBattle();
        gameState.nextPlayer();
        onBroadcast.run();
    }
}

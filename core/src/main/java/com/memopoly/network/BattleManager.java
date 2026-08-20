package com.memopoly.network;

import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Meme;
import com.memopoly.game.model.Player;
import com.memopoly.network.packets.BattleResponsePacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.memopoly.game.model.GameState.BattlePhase.*;
import static com.memopoly.game.model.GameState.BattleType.MEME_BATTLE_CELL;

/**
 * Менеджер мем-баттла: управляет полным циклом фаз баттла (приглашения, сбор мемов, голосование, подсчёт результатов и выдача банка).
 */
public class BattleManager {

    private final GameState gameState;
    private final List<BoardCell> boardCells;
    private final Runnable onBroadcast;
    private final ScheduledExecutorService timerExecutor;
    private final Object stateLock;
    private ScheduledFuture<?> currentTimerTask;

    private int battleBank;
    private int battleOwnerId;
    private int battleCellIndex;
    private ArrayList<Integer> battleInvited;

    public BattleManager(GameState gameState, List<BoardCell> board, Runnable onBroadcast, ScheduledExecutorService timerExecutor, Object stateLock) {
        this.gameState = gameState;
        this.boardCells = board;
        this.onBroadcast = onBroadcast;
        this.timerExecutor = timerExecutor;
        this.stateLock = stateLock;
        this.battleInvited = new ArrayList<>();
    }
    /**
     * Called when a player lands on a MEME_BATTLE cell.
     * Enters BATTLE_SETUP so the organizer can pick topic and stakes.
     */
    public void startSetup(int organizerId, int cellIndex) {
        gameState.currentPhase = GameState.GamePhase.MEME_BATTLE;
        gameState.isInBattle = true;
        gameState.battleType = MEME_BATTLE_CELL;
        gameState.battleOwnerId = organizerId;
        gameState.battleSetupCellIndex = cellIndex;
        battleCellIndex = cellIndex;
        battleOwnerId = organizerId;
        battleBank = 0;

        if (gameState.battleParticipants == null) gameState.battleParticipants = new ArrayList<>();
        if (gameState.battleInvited == null) gameState.battleInvited = new ArrayList<>();
        if (gameState.battleVoters == null) gameState.battleVoters = new ArrayList<>();
        gameState.battleParticipants.clear();
        gameState.battleInvited.clear();
        gameState.battleVoters.clear();
        gameState.battleMemes.clear();
        gameState.votes.clear();

        gameState.battlePhase = BATTLE_SETUP;
        gameState.battleTimerSeconds = 60;
        gameState.battleTopic = "";
        gameState.battleStakes = 0;
        gameState.battleBank = 0;

        onBroadcast.run();
    }

    /**
     * Called when the organizer sends START_MEME_BATTLE with a chosen topic and stakes.
     * Deducts the owner's stakes and moves to INVITE phase.
     */
    public void confirmSetup(int organizerId, String topic, int stakes) {
        if (gameState.battlePhase != BATTLE_SETUP || gameState.battleOwnerId != organizerId) return;

        gameState.battleTopic = topic;
        gameState.battleStakes = stakes;

        // Organizer pays their stake
        if (stakes > 0) {
            Player organizer = gameState.getPlayerById(organizerId);
            if (organizer != null) {
                organizer.pay(stakes);
                battleBank += stakes;
                gameState.battleBank = battleBank;
            }
        }

        startBattle(organizerId, battleCellIndex);
    }

    public void startBattle(int organizerId, int cellIndex) {
        gameState.currentPhase = GameState.GamePhase.MEME_BATTLE;
        gameState.isInBattle = true;
        battleOwnerId = organizerId;
        battleCellIndex = cellIndex;

        // battleBank already set via confirmSetup

        battleInvited.clear();
        for (Player player : gameState.players) {
            if (player.id != organizerId && !player.isBankrupt) {
                battleInvited.add(player.id);
            }
        }

        gameState.battleOwnerId = organizerId;
        gameState.battlePhase = INVITE;
        gameState.battleTimerSeconds = 30;
        gameState.battleBank = battleBank;
        gameState.battleInvited = battleInvited;
        gameState.battleParticipants.clear();
        gameState.battleParticipants.add(organizerId);

        gameState.battleMemes.clear();
        gameState.votes.clear();
        if (gameState.battleVoters == null) gameState.battleVoters = new ArrayList<>();
        gameState.battleVoters.clear();

        onBroadcast.run();

        startPhaseTimer(this::checkInvitePhaseCompletion);
    }

    public void handleBattleResponse(BattleResponsePacket packet) {
        if (!battleInvited.contains(packet.playerId)) return;

        if (packet.accepted) {
            gameState.battleParticipants.add(packet.playerId);
            Player player = gameState.getPlayerById(packet.playerId);
            if (player != null && gameState.battleStakes > 0) {
                player.pay(gameState.battleStakes);
                battleBank += gameState.battleStakes;
                gameState.battleBank = battleBank;
            }
        }

        battleInvited.remove(Integer.valueOf(packet.playerId));
        gameState.battleInvited = battleInvited;

        checkInvitePhaseCompletion();
    }

    private void checkInvitePhaseCompletion() {
        if (battleInvited.isEmpty() || gameState.battleTimerSeconds <= 0) {
            cancelCurrentTimer();
            if (gameState.battleParticipants.size() < 2) {
                skipBattle("Мем-баттл отменён: недостаточно участников");
                return;
            }
            startCollecting();
        } else {
            onBroadcast.run();
        }
    }

    public void startCollecting() {
        gameState.battlePhase = COLLECTING_MEMES;
        gameState.battleTimerSeconds = 60;
        onBroadcast.run();
        startPhaseTimer(this::checkCollectingPhaseCompletion);
    }
    public void checkCollectingPhaseCompletion() {
        if (gameState.battleMemes.size() >= gameState.battleParticipants.size() || gameState.battleTimerSeconds <= 0) {
            cancelCurrentTimer();

            if (gameState.battleParticipants.size() == 2 && gameState.battleMemes.size() == 2) {
                injectBotMeme();
            }

            startVoting();
        } else {
            onBroadcast.run();
        }
    }

    private void injectBotMeme() {
        if (gameState.memeDeckDrawPile.isEmpty()) {
            return;
        }
        Meme botMeme = gameState.memeDeckDrawPile.remove(0);
        botMeme.ownerId = -999;
        gameState.battleMemes.add(botMeme);
    }

    public void startVoting() {
        if (gameState.battleMemes.size() < 2) {
            skipBattle("Мем-баттл отменён: выбрано меньше двух мемов");
            return;
        }

        gameState.battleParticipants = collectSubmittedParticipants();
        gameState.battlePhase = VOTING;
        gameState.battleTimerSeconds = 30;
        onBroadcast.run();
        startPhaseTimer(this::checkVotingPhaseCompletion);
    }

    public void checkVotingPhaseCompletion() {
        if (gameState.battleVoters.size() >= gameState.players.size() || gameState.battleTimerSeconds <= 0) {
            cancelCurrentTimer();
            countVotes();
        } else {
            onBroadcast.run();
        }
    }

    private void startPhaseTimer(Runnable completionCheck) {
        cancelCurrentTimer();
        currentTimerTask = timerExecutor.scheduleAtFixedRate(() -> {
            synchronized (stateLock) {
                if (!gameState.isInBattle) {
                    cancelCurrentTimer();
                    return;
                }
                gameState.battleTimerSeconds--;
                completionCheck.run();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void cancelCurrentTimer() {
        if (currentTimerTask != null && !currentTimerTask.isCancelled()) {
            currentTimerTask.cancel(false);
        }
    }

    public void cancelSetup(int organizerId) {
        if (gameState.battlePhase != BATTLE_SETUP || gameState.battleOwnerId != organizerId) return;
        skipBattle("Мем-баттл отменён организатором");
    }

    private void normalizeBattleParticipantsToSubmittedMemes() {
        gameState.battleParticipants = collectSubmittedParticipants();
    }

    private int countSubmittedParticipants() {
        return collectSubmittedParticipants().size();
    }

    private ArrayList<Integer> collectSubmittedParticipants() {
        ArrayList<Integer> submittedParticipants = new ArrayList<>();
        for (Meme meme : gameState.battleMemes) {
            if (!submittedParticipants.contains(meme.ownerId)) {
                submittedParticipants.add(meme.ownerId);
            }
        }
        return submittedParticipants;
    }

    private void skipBattle(String reason) {
        if (gameState.battleType == MEME_BATTLE_CELL && gameState.battleStakes > 0) {
            for (Integer participantId : gameState.battleParticipants) {
                if (participantId == null || participantId == gameState.battleOwnerId) {
                    continue;
                }
                Player participant = gameState.getPlayerById(participantId);
                if (participant != null) {
                    participant.receive(gameState.battleStakes);
                }
            }
            battleBank = 0;
            gameState.battleBank = 0;
        }
        gameState.lastActionLog = reason;
        gameState.endMemeBattle();
        gameState.nextPlayer();
        gameState.lastActionLog = reason;
        onBroadcast.run();
    }

    public void countVotes() {
        // Evaluate bot logic for 2-player battles first
        if (gameState.battleParticipants.size() == 2) {
            Meme botMeme = null;
            for (Meme m : gameState.battleMemes) {
                if (m.ownerId == -999) {
                    botMeme = m;
                    break;
                }
            }
            if (botMeme != null) {
                int botVotes = gameState.votes.getOrDefault(botMeme.id, 0);
                if (botVotes > 0) {
                    if (botVotes >= 2) {
                        // Both voted for bot. Bot wins. They draw with 0 winnings.
                        handleDraw(Collections.singletonList(botMeme.id));
                        return;
                    }
                    // Bot has exactly 1 vote. The other human voted for.
                    // Who voted for the bot? The human with 1 vote!
                    Meme humanWithVote = null;
                    Meme humanWithZero = null;
                    for (Meme m : gameState.battleMemes) {
                        if (m.ownerId != -999) {
                            if (gameState.votes.getOrDefault(m.id, 0) > 0) {
                                humanWithVote = m;
                            } else {
                                humanWithZero = m;
                            }
                        }
                    }
                    if (humanWithVote != null && humanWithZero != null) {
                        // humanWithVote voted for Bot. So humanWithZero wins!
                        handleWinner(humanWithZero.id);
                        return;
                    } else if (humanWithVote == null && humanWithZero != null) {
                        // Only 1 person voted (the one in battleVoters). They voted for Bot.
                        // So the other person wins. But wait, how do we know who is who?
                        if (!gameState.battleVoters.isEmpty()) {
                            int voter = gameState.battleVoters.get(0);
                            for (Meme m : gameState.battleMemes) {
                                if (m.ownerId != -999 && m.ownerId != voter) {
                                    handleWinner(m.id);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

        int max = 0;
        for (int votes : gameState.votes.values()) {
            if (votes > max) max = votes;
        }

        ArrayList<Integer> winners = new ArrayList<>();
        if (max == 0) {
            for (Meme meme : gameState.battleMemes) {
                winners.add(meme.id);
            }
        } else {
            for (Map.Entry<Integer, Integer> entry : gameState.votes.entrySet()) {
                if (entry.getValue() == max) {
                    winners.add(entry.getKey());
                }
            }
        }

        if (winners.size() == 1) {
            handleWinner(winners.get(0));
        } else {
            handleDraw(winners);
        }
    }

    public void handleWinner(int memeId) {
        Meme winnerMeme = null;
        for (Meme meme : gameState.battleMemes) {
            if (meme.id == memeId) {
                winnerMeme = meme;
                break;
            }
        }

        // who submitted a meme (as compensation/bonus), including the winner!
        for (Meme meme : gameState.battleMemes) {
            if (meme.ownerId == -999) continue;
            Player player = gameState.getPlayerById(meme.ownerId);
            if (player != null) {
                int votesForMeme = gameState.votes.getOrDefault(meme.id, 0);
                if (votesForMeme > 0) {
                    player.receive(votesForMeme * 10);
                }
            }
        }

        if (winnerMeme != null && winnerMeme.ownerId != -999) {
            Player winner = gameState.getPlayerById(winnerMeme.ownerId);
            winner.money += battleBank;
            gameState.lastActionLog = winner.name + " победил в мем-баттле и получил " + battleBank + " монет!";
        } else {
            gameState.lastActionLog = "Победил мем бота. Банк сгорает!";
        }

        gameState.battlePhase = RESULTS;
        onBroadcast.run();
        scheduleEndBattle();
    }
    public void handleDraw(List<Integer> tiedMemeIds) {
        ArrayList<Integer> tiedOwnerIds = new ArrayList<>();
        for (Meme meme : gameState.battleMemes) {
            if (tiedMemeIds.contains(meme.id) && meme.ownerId != -999) {
                if (!tiedOwnerIds.contains(meme.ownerId)) {
                    tiedOwnerIds.add(meme.ownerId);
                }
            }

            if (meme.ownerId != -999) {
                Player player = gameState.getPlayerById(meme.ownerId);
                if (player != null) {
                    int votesForMeme = gameState.votes.getOrDefault(meme.id, 0);
                    if (votesForMeme > 0) {
                        player.receive(votesForMeme * 10);
                    }
                }
            }
        }

        if (tiedOwnerIds.isEmpty()) {
            gameState.lastActionLog = "Мем-баттл завершился вничью с ботом. Никто не получает банк!";
            gameState.battlePhase = RESULTS;
            onBroadcast.run();
            scheduleEndBattle();
            return;
        }

        int ownerCellId = gameState.cellOwners.getOrDefault(battleCellIndex, -1);
        Player ownerCell = gameState.getPlayerById(ownerCellId);

        int share = battleBank / tiedOwnerIds.size();
        for (Integer ownerId : tiedOwnerIds) {
            Player player = gameState.getPlayerById(ownerId);
            if (player != null) {
                player.money += share;
            }
        }
        gameState.battlePhase = RESULTS;
        gameState.lastActionLog = "Мем-баттл завершился вничью! Банк разделён.";
        onBroadcast.run();
        scheduleEndBattle();
    }

    private void scheduleEndBattle() {
        timerExecutor.schedule(() -> {
            synchronized (stateLock) {
                if (gameState.battlePhase != RESULTS) {
                    return;
                }
                gameState.endMemeBattle();
                gameState.nextPlayer();
                onBroadcast.run();
            }
        }, 4, TimeUnit.SECONDS);
    }
}

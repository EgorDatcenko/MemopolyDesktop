package com.memopoly.network;

import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Meme;
import com.memopoly.game.model.Player;
import com.memopoly.network.packets.BattleResponsePacket;
import com.memopoly.network.twitch.TwitchChatService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.memopoly.game.model.GameState.BattlePhase.*;
import static com.memopoly.game.model.GameState.BattleType.MEME_BATTLE_CELL;

/**
 * Менеджер мем-баттла: управляет полным циклом фаз баттла (приглашения, сбор
 * мемов, голосование, подсчёт результатов и выдача банка).
 */
public class BattleManager {

    private final GameState gameState;
    private final List<BoardCell> boardCells;
    private final Runnable onBroadcast;
    private final ScheduledExecutorService timerExecutor;
    private final Object stateLock;
    private ScheduledFuture<?> currentTimerTask;
    private volatile boolean phaseTransitionInProgress = false;

    private int battleBank;
    private int battleOwnerId;
    private int battleCellIndex;
    private ArrayList<Integer> battleInvited;
    private TwitchChatService twitchChatService;

    public void setTwitchChatService(TwitchChatService service) {
        this.twitchChatService = service;
    }

    public BattleManager(GameState gameState, List<BoardCell> board, Runnable onBroadcast,
            ScheduledExecutorService timerExecutor, Object stateLock) {
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
        // Отмена активной сделки при начале баттла
        if (gameState.tradeId != 0) {
            gameState.tradeId = 0;
            gameState.tradeProposerId = -1;
            gameState.tradeTargetId = -1;
            gameState.tradeProposerCells.clear();
            gameState.tradeTargetCells.clear();
            gameState.tradeProposerMoney = 0;
            gameState.tradeTargetMoney = 0;
        }

        gameState.currentPhase = GameState.GamePhase.MEME_BATTLE;
        gameState.isInBattle = true;
        gameState.battleType = MEME_BATTLE_CELL;
        gameState.battleOwnerId = organizerId;
        gameState.battleSetupCellIndex = cellIndex;
        battleCellIndex = cellIndex;
        battleOwnerId = organizerId;
        battleBank = 0;

        if (gameState.battleParticipants == null)
            gameState.battleParticipants = new ArrayList<>();
        if (gameState.battleInvited == null)
            gameState.battleInvited = new ArrayList<>();
        if (gameState.battleVoters == null)
            gameState.battleVoters = new ArrayList<>();
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
     * Called when the organizer sends START_MEME_BATTLE with a chosen topic and
     * stakes.
     * Deducts the owner's stakes and moves to INVITE phase.
     */
    public void confirmSetup(int organizerId, String topic, int stakes) {
        if (gameState.battlePhase != BATTLE_SETUP || gameState.battleOwnerId != organizerId)
            return;

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
        phaseTransitionInProgress = false;
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
        gameState.recordAchievement(organizerId, "FIRST_BATTLE");

        gameState.battleMemes.clear();
        gameState.votes.clear();
        if (gameState.battleVoters == null)
            gameState.battleVoters = new ArrayList<>();
        gameState.battleVoters.clear();

        onBroadcast.run();

        startPhaseTimer(this::checkInvitePhaseCompletion);
    }

    public void handleBattleResponse(BattleResponsePacket packet) {
        if (!battleInvited.contains(packet.playerId))
            return;

        if (packet.accepted) {
            gameState.battleParticipants.add(packet.playerId);
            gameState.recordAchievement(packet.playerId, "FIRST_BATTLE");
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
        // State Guard: отсекает «хвостовые» тики если фаза уже переключилась
        if (gameState.battlePhase != INVITE)
            return;
        if (phaseTransitionInProgress)
            return;
        if (battleInvited.isEmpty() || gameState.battleTimerSeconds <= 0) {
            phaseTransitionInProgress = true;
            try {
                cancelCurrentTimer();
                // Все, кто не ответил, считаются отказавшимися
                battleInvited.clear();
                gameState.battleInvited = battleInvited;
                if (gameState.battleParticipants.size() < 2) {
                    skipBattle("Мем-баттл отменён: недостаточно участников");
                    return;
                }
                startCollecting();
            } finally {
                phaseTransitionInProgress = false;
            }
        } else {
            onBroadcast.run();
        }
    }

    public void startCollecting() {
        phaseTransitionInProgress = false;
        gameState.battlePhase = COLLECTING_MEMES;
        gameState.battleTimerSeconds = 30;
        if (gameState.juryOrder != null && !gameState.juryOrder.isEmpty()) {
            int nextIdx = (gameState.juryOrder.indexOf(gameState.themeContinuatorId) + 1) % gameState.juryOrder.size();
            gameState.themeContinuatorId = gameState.juryOrder.get(nextIdx);
        } else {
            gameState.themeContinuatorId = -1;
        }
        gameState.themeContinuationUsed = false;
        onBroadcast.run();
        startPhaseTimer(this::checkCollectingPhaseCompletion);
    }

    public void checkCollectingPhaseCompletion() {
        // State Guard: отсекает «хвостовые» тики если фаза уже переключилась
        if (gameState.battlePhase != COLLECTING_MEMES)
            return;
        if (phaseTransitionInProgress)
            return;
        if (gameState.battleMemes.size() >= gameState.battleParticipants.size()
                || gameState.battleTimerSeconds <= 0) {
            phaseTransitionInProgress = true;
            try {
                cancelCurrentTimer();
                // При таймауте принудительно выдаём мемы не успевшим
                autoSubmitMissingMemes();
                startVoting();
            } finally {
                phaseTransitionInProgress = false;
            }
        } else {
            onBroadcast.run();
        }
    }

    private void autoSubmitMissingMemes() {
        if (gameState.battleParticipants == null)
            return;
        for (int participantId : new ArrayList<>(gameState.battleParticipants)) {
            boolean submitted = false;
            for (Meme m : gameState.battleMemes) {
                if (m.ownerId == participantId) {
                    submitted = true;
                    break;
                }
            }
            if (!submitted) {
                Player p = gameState.getPlayerById(participantId);
                if (p != null && p.handMemes != null && !p.handMemes.isEmpty()) {
                    Meme chosen = p.handMemes.get(0);
                    chosen.ownerId = p.id;
                    gameState.battleMemes.add(chosen);
                }
            }
        }
    }

    public void startVoting() {
        phaseTransitionInProgress = false;
        if (gameState.battleMemes.size() < 2) {
            skipBattle("Мем-баттл отменён: выбрано меньше двух мемов");
            return;
        }

        gameState.battleParticipants = collectSubmittedParticipants();
        gameState.battlePhase = VOTING;
        gameState.battleTimerSeconds = 30;
        if (twitchChatService != null && twitchChatService.isConnected()) {
            twitchChatService.startVotingSession();
        }
        onBroadcast.run();
        startPhaseTimer(this::checkVotingPhaseCompletion);
    }

    public void checkVotingPhaseCompletion() {
        // State Guard: отсекает «хвостовые» тики если фаза уже переключилась
        if (gameState.battlePhase != VOTING)
            return;
        if (phaseTransitionInProgress)
            return;
        if (gameState.battleVoters.size() >= gameState.players.size()
                || gameState.battleTimerSeconds <= 0) {
            phaseTransitionInProgress = true;
            try {
                cancelCurrentTimer();
                countVotes();
            } finally {
                phaseTransitionInProgress = false;
            }
        } else {
            onBroadcast.run();
        }
    }

    private void startPhaseTimer(Runnable completionCheck) {
        cancelCurrentTimer();
        if (timerExecutor != null && !timerExecutor.isShutdown() && !timerExecutor.isTerminated()) {
            try {
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
            } catch (java.util.concurrent.RejectedExecutionException ignored) {
                currentTimerTask = null;
            }
        }
    }

    private void cancelCurrentTimer() {
        if (currentTimerTask != null && !currentTimerTask.isCancelled()) {
            currentTimerTask.cancel(false);
        }
    }

    public void cancelSetup(int organizerId) {
        if (gameState.battlePhase != BATTLE_SETUP || gameState.battleOwnerId != organizerId)
            return;
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
        if (gameState.twitchVotes == null) {
            gameState.twitchVotes = new HashMap<>();
        }
        gameState.twitchVotes.clear();

        if (twitchChatService != null && twitchChatService.isConnected() && gameState.battleMemes != null) {
            int memeCount = gameState.battleMemes.size();
            for (int i = 0; i < memeCount; i++) {
                int slot = i + 1;
                int slotVotes = twitchChatService.getVotesForSlot(slot);
                gameState.twitchVotes.put(gameState.battleMemes.get(i).id, slotVotes);
            }
            int majoritySlot = twitchChatService.getMajoritySlot(memeCount);
            if (majoritySlot >= 1 && majoritySlot <= memeCount) {
                Meme bonusMeme = gameState.battleMemes.get(majoritySlot - 1);
                gameState.votes.put(bonusMeme.id, gameState.votes.getOrDefault(bonusMeme.id, 0) + 1);
            }
        }

        if (gameState.juryVoteCount != null) {
            for (Map.Entry<Integer, Integer> entry : gameState.juryVoteCount.entrySet()) {
                if (entry.getValue() >= 2) {
                    gameState.recordAchievement(entry.getKey(), "CRITIC");
                }
            }
        }


        int max = 0;
        for (int votes : gameState.votes.values()) {
            if (votes > max)
                max = votes;
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
        } else if (winners.size() > 1 && gameState.juryVotes != null && !gameState.juryVotes.isEmpty()) {
            Map<Integer, Integer> juryCounts = new HashMap<>();
            for (int tiedMemeId : winners) {
                int count = 0;
                for (int juryVoteMemeId : gameState.juryVotes.values()) {
                    if (juryVoteMemeId == tiedMemeId)
                        count++;
                }
                juryCounts.put(tiedMemeId, count);
            }

            int maxJury = -1;
            for (int count : juryCounts.values()) {
                if (count > maxJury)
                    maxJury = count;
            }

            ArrayList<Integer> juryWinners = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : juryCounts.entrySet()) {
                if (entry.getValue() == maxJury) {
                    juryWinners.add(entry.getKey());
                }
            }

            if (juryWinners.size() == 1) {
                int winnerMemeId = juryWinners.get(0);
                for (Map.Entry<Integer, Integer> jv : gameState.juryVotes.entrySet()) {
                    if (jv.getValue() == winnerMemeId) {
                        gameState.recordAchievement(jv.getKey(), "KINGMAKER");
                    }
                }
                handleWinner(winnerMemeId);
            } else {
                Collections.shuffle(juryWinners);
                int winnerMemeId = juryWinners.get(0);
                gameState.lastActionLog = "Победитель определён жребием";
                handleWinner(winnerMemeId);
            }
        } else {
            Collections.shuffle(winners);
            int winnerMemeId = winners.get(0);
            gameState.lastActionLog = "Победитель определён жребием";
            handleWinner(winnerMemeId);
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
            Player player = gameState.getPlayerById(meme.ownerId);
            if (player != null) {
                int votesForMeme = gameState.votes.getOrDefault(meme.id, 0);
                if (votesForMeme > 0) {
                    player.receive(votesForMeme * 10);
                }
            }
        }

        if (winnerMeme != null) {
            Player winner = gameState.getPlayerById(winnerMeme.ownerId);
            if (winner != null) {
                winner.money += battleBank;
                winner.battleWins++;
                if (winner.battleWins >= 3) {
                    gameState.recordAchievement(winner.id, "MEME_LORD");
                }
                gameState.lastActionLog = winner.name + " победил и забирает " + battleBank + " монет!";
            }
        }

        gameState.battleWinnerMemeId = memeId;
        gameState.battleWinnerPlayerId = (winnerMeme != null) ? winnerMeme.ownerId : -1;
        gameState.battlePrizeAmount = battleBank;

        gameState.battlePhase = RESULTS;
        onBroadcast.run();
        scheduleEndBattle();
    }

    public void handleDraw(List<Integer> tiedMemeIds) {
        ArrayList<Integer> tiedOwnerIds = new ArrayList<>();
        for (Meme meme : gameState.battleMemes) {
            if (tiedMemeIds.contains(meme.id)) {
                if (!tiedOwnerIds.contains(meme.ownerId)) {
                    tiedOwnerIds.add(meme.ownerId);
                }
            }

            Player player = gameState.getPlayerById(meme.ownerId);
            if (player != null) {
                int votesForMeme = gameState.votes.getOrDefault(meme.id, 0);
                if (votesForMeme > 0) {
                    player.receive(votesForMeme * 10);
                }
            }
        }

        if (tiedOwnerIds.isEmpty()) {
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
        gameState.battleWinnerMemeId = (tiedMemeIds != null && !tiedMemeIds.isEmpty()) ? tiedMemeIds.get(0) : -1;
        gameState.battleWinnerPlayerId = -1;
        gameState.battlePrizeAmount = battleBank;

        gameState.battlePhase = RESULTS;
        gameState.lastActionLog = "Мем-баттл завершился вничью! Банк разделён.";
        onBroadcast.run();
        scheduleEndBattle();
    }

    private void scheduleEndBattle() {
        if (timerExecutor != null && !timerExecutor.isShutdown() && !timerExecutor.isTerminated()) {
            try {
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
            } catch (java.util.concurrent.RejectedExecutionException ignored) {
            }
        }
    }
}

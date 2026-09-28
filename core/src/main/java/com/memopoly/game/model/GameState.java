package com.memopoly.game.model;

import java.util.*;

/**
 * Главная модель состояния матча: содержит список игроков, фазу игры, состояние
 * аукционов и мем-баттлов, лог событий и баланс банка мемов.
 */
public class GameState {
    public HashMap<Integer, Integer> cellOwners;
    public HashMap<Integer, Boolean> cellMortgaged;
    public boolean testMode = false;
    /** Number of branches built on each situation cell (0..4). */
    public HashMap<Integer, Integer> cellHouses;

    public enum GamePhase {
        WAITING,
        PLAYING,
        ROLLING_DICE,
        PLAYER_ACTION,
        AUCTION,
        GAME_OVER,
        MEME_BANK_ACTION,
        MEME_BATTLE
    }

    public enum BattleType {
        MEME_BATTLE_CELL
    }

    public enum BattlePhase {
        BATTLE_SETUP,
        INVITE,
        COLLECTING_MEMES,
        VOTING,
        COUNTING,
        RESULTS
    }

    public ArrayList<Player> players;
    public String roomCode;
    public ArrayList<String> achievementEvents = new ArrayList<>();
    public int currentPlayerIndex;
    public GamePhase currentPhase;
    public int winnerId = -1;
    public boolean isGameOver = false;
    public int diceValue;
    public boolean hasRolledThisTurn;
    public String lastActionLog;
    public String notificationText;
    public long notificationTimestamp;
    public int turnCount;
    public String selectedDeckName;
    public ArrayList<Meme> memeDeckDrawPile;

    public boolean isInBattle;
    public int battleStakes;
    public String battleTopic;
    public ArrayList<Meme> battleMemes;
    public HashMap<Integer, Integer> votes;
    public int battleOwnerId;

    public BattleType battleType;
    public int battleSetupCellIndex; // cell that triggered the battle setup
    public ArrayList<Integer> battleParticipants;
    public ArrayList<Integer> battleInvited;
    public HashMap<Integer, Boolean> battleAccepted;
    public int battleTimerSeconds;
    public BattlePhase battlePhase;
    public int battleBank;
    public ArrayList<Integer> battleVoters;
    public HashMap<Integer, Integer> twitchVotes;
    public int battleWinnerMemeId;
    public int battleWinnerPlayerId;
    public int battlePrizeAmount;
    public boolean isInAuction;
    public int auctionCellId;
    public HashMap<Integer, Integer> auctionBids;
    public int currentAuctionTime;
    public int auctionStarterPlayerId;
    public int auctionCurrentPlayerId;

    public int memeBankPlayerId = -1;

    public boolean rolesEnabled = false;
    public boolean botMemeEnabled = false;
    public HashMap<Integer, String> playerRoles = new HashMap<>();
    public HashMap<Integer, Boolean> roleUsedThisRound = new HashMap<>();
    public Set<Integer> rerollUsedThisRound = new HashSet<>();
    public boolean awaitingReroll = false;
    public int rerollPlayerId = -1;
    public int rerollOrigin = -1;
    public boolean moderatorChoicePending = false;
    public int moderatorPlayerId = -1;

    public Set<Integer> juryIds = new HashSet<>();
    public Map<Integer, Integer> juryVotes = new HashMap<>();
    public Map<Integer, Integer> juryVoteCount = new HashMap<>();
    public Map<Integer, Boolean> bankruptEventUsedRound = new HashMap<>();
    public List<Integer> juryOrder = new ArrayList<>();
    public int themeContinuatorId = -1;
    public boolean themeContinuationUsed = false;

    // Trade fields
    public int tradeId = 0; // 0 = нет активной сделки
    public int tradeProposerId = -1; // ID инициатора сделки
    public int tradeTargetId = -1; // ID цели сделки
    public ArrayList<Integer> tradeProposerCells = new ArrayList<>(); // Клетки инициатора
    public ArrayList<Integer> tradeTargetCells = new ArrayList<>(); // Клетки цели
    public int tradeProposerMoney = 0; // Монеты инициатора
    public int tradeTargetMoney = 0; // Монеты цели

    // Debt fields
    public int pendingDebtPlayerId = -1;
    public int pendingDebtAmount = 0;
    public int pendingDebtRecipientId = -1;

    public GameState() {
        this.cellOwners = new HashMap<>();
        this.cellMortgaged = new HashMap<>();
        this.cellHouses = new HashMap<>();
        this.players = new ArrayList<>();
        this.achievementEvents = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.currentPhase = GamePhase.WAITING;
        this.diceValue = 0;
        this.hasRolledThisTurn = false;
        this.lastActionLog = "Игра началась";
        this.notificationText = "";
        this.notificationTimestamp = 0L;
        this.turnCount = 0;
        this.selectedDeckName = null;
        this.memeDeckDrawPile = new ArrayList<>();
        this.roomCode = null;
        this.isInBattle = false;
        this.battleMemes = new ArrayList<>();
        this.battleParticipants = new ArrayList<>();
        this.battleInvited = new ArrayList<>();
        this.battleAccepted = new HashMap<>();
        this.votes = new HashMap<>();
        this.battleOwnerId = -1;
        this.battleVoters = new ArrayList<>();
        this.twitchVotes = new HashMap<>();
        this.battleWinnerMemeId = -1;
        this.battleWinnerPlayerId = -1;
        this.battlePrizeAmount = 0;
        this.isInAuction = false;
        this.auctionBids = new HashMap<>();
        this.auctionCellId = -1;
        this.auctionStarterPlayerId = -1;
        this.auctionCurrentPlayerId = -1;
    }

    public Player getCurrentPlayer() {
        if (players.isEmpty())
            return null;
        return players.get(currentPlayerIndex);
    }

    public void nextPlayer() {
        int oldIndex = currentPlayerIndex;
        boolean wrapped = false;
        int checkedPlayers = 0;
        do {
            int next = (currentPlayerIndex + 1) % players.size();
            if (next <= oldIndex) {
                wrapped = true; // перешли через конец списка — круг замкнулся
            }
            currentPlayerIndex = next;
            Player candidate = players.get(currentPlayerIndex);
            if (!candidate.isBankrupt && candidate.skipNextTurn) {
                candidate.skipNextTurn = false;
                lastActionLog = candidate.name + " пропускает ход";
                checkedPlayers++;
                continue;
            }
            if (!candidate.isBankrupt) {
                break;
            }
            checkedPlayers++;
        } while (checkedPlayers <= players.size());

        // Сброс «переброса» в игре при обходе круга
        if (wrapped) {
            rerollUsedThisRound.clear();
        }

        awaitingReroll = false;
        rerollPlayerId = -1;
        rerollOrigin = -1;
        moderatorChoicePending = false;
        moderatorPlayerId = -1;

        turnCount++;
        diceValue = 0;
        hasRolledThisTurn = false;
        memeBankPlayerId = -1;
        currentPhase = GamePhase.PLAYING;
        Player curr = getCurrentPlayer();
        if (curr != null) {
            curr.consecutiveDoubles = 0;
        }
        lastActionLog = "Ход переходит к " + (curr != null ? curr.name : "?");
    }

    public void addPlayer(Player player) {
        players.add(player);
        lastActionLog = "Игрок " + player.name + " присоединился";
    }

    public void showNotification(String text) {
        notificationText = text == null ? "" : text;
        notificationTimestamp = System.currentTimeMillis();
    }

    public void removePlayer(int playerId) {
        players.removeIf(p -> p.id == playerId);
        lastActionLog = "Игрок покинул игру";
    }

    public Player getPlayerById(int id) {
        for (Player p : players) {
            if (p.id == id)
                return p;
        }
        return null;
    }

    public void recordAchievement(int playerId, String achievementId) {
        String event = playerId + "|" + achievementId;
        if (!achievementEvents.contains(event)) {
            achievementEvents.add(event);
        }
    }

    public boolean isGameOver() {
        int activePlayers = 0;
        for (Player p : players) {
            if (!p.isBankrupt)
                activePlayers++;
        }
        return activePlayers <= 1;
    }

    public Player getWinner() {
        if (!isGameOver())
            return null;

        Player richest = null;
        for (Player p : players) {
            if (!p.isBankrupt && (richest == null || p.money > richest.money)) {
                richest = p;
            }
        }
        return richest;
    }

    public void startMemeBattle(int stakes, String topic, int ownerId) {
        isInBattle = true;
        battleStakes = stakes;
        battleTopic = topic;
        battleOwnerId = ownerId;
        battleMemes.clear();
        votes.clear();
        currentPhase = GamePhase.MEME_BATTLE;
        lastActionLog = "Начинается мем-баттл! Тема: " + topic;
    }

    public boolean containsMeme(int targetId) {
        for (Meme i : battleMemes) {
            if (i.id == targetId) {
                return true;
            }
        }
        return false;
    }

    public boolean isMemeOwnedBy(int memeId, int ownerId) {
        for (Meme meme : battleMemes) {
            if (meme.id == memeId && meme.ownerId == ownerId) {
                return true;
            }
        }
        return false;
    }

    public void endMemeBattle() {
        isInBattle = false;
        battleStakes = 0;
        battleTopic = "";
        battleOwnerId = -1;
        battleMemes.clear();
        votes.clear();
        if (twitchVotes != null) twitchVotes.clear();
        battleWinnerMemeId = -1;
        battleWinnerPlayerId = -1;
        battlePrizeAmount = 0;
        currentPhase = GamePhase.PLAYING;
        battleVoters.clear();
    }

    public void startAuction(int cellId) {
        isInAuction = true;
        auctionCellId = cellId;
        auctionBids.clear();
        currentAuctionTime = 30;
        auctionCurrentPlayerId = -1;
        currentPhase = GamePhase.AUCTION;
        lastActionLog = "Начинается аукцион!";
    }

    public void endAuction() {
        isInAuction = false;
        auctionCellId = -1;
        auctionBids.clear();
        auctionStarterPlayerId = -1;
        auctionCurrentPlayerId = -1;
        currentAuctionTime = 30;
        currentPhase = GamePhase.PLAYING;
    }

    public void remapPlayerId(int oldId, int newId) {
        if (oldId == newId) return;
        Player p = getPlayerById(oldId);
        if (p != null) {
            p.id = newId;
            p.isConnected = true;
        }
        for (Player other : players) {
            if (other.tradedWith != null && other.tradedWith.contains(oldId)) {
                other.tradedWith.remove(oldId);
                other.tradedWith.add(newId);
            }
        }
        if (cellOwners != null) {
            for (Map.Entry<Integer, Integer> entry : cellOwners.entrySet()) {
                if (entry.getValue() != null && entry.getValue() == oldId) {
                    entry.setValue(newId);
                }
            }
        }
        if (winnerId == oldId) winnerId = newId;
        if (playerRoles != null && playerRoles.containsKey(oldId)) {
            playerRoles.put(newId, playerRoles.remove(oldId));
        }
        if (roleUsedThisRound != null && roleUsedThisRound.containsKey(oldId)) {
            roleUsedThisRound.put(newId, roleUsedThisRound.remove(oldId));
        }
        if (rerollUsedThisRound != null && rerollUsedThisRound.contains(oldId)) {
            rerollUsedThisRound.remove(oldId);
            rerollUsedThisRound.add(newId);
        }
        if (rerollPlayerId == oldId) rerollPlayerId = newId;
        if (moderatorPlayerId == oldId) moderatorPlayerId = newId;
        if (memeBankPlayerId == oldId) memeBankPlayerId = newId;

        // Meme battle
        if (battleOwnerId == oldId) battleOwnerId = newId;
        if (battleParticipants != null) {
            Collections.replaceAll(battleParticipants, oldId, newId);
        }
        if (battleInvited != null) {
            Collections.replaceAll(battleInvited, oldId, newId);
        }
        if (battleAccepted != null && battleAccepted.containsKey(oldId)) {
            battleAccepted.put(newId, battleAccepted.remove(oldId));
        }
        if (battleVoters != null) {
            Collections.replaceAll(battleVoters, oldId, newId);
        }
        if (votes != null && votes.containsKey(oldId)) {
            votes.put(newId, votes.remove(oldId));
        }
        if (battleMemes != null) {
            for (Meme m : battleMemes) {
                if (m.ownerId == oldId) {
                    m.ownerId = newId;
                }
            }
        }

        // Auction
        if (auctionStarterPlayerId == oldId) auctionStarterPlayerId = newId;
        if (auctionCurrentPlayerId == oldId) auctionCurrentPlayerId = newId;
        if (auctionBids != null && auctionBids.containsKey(oldId)) {
            auctionBids.put(newId, auctionBids.remove(oldId));
        }

        // Trade
        if (tradeProposerId == oldId) tradeProposerId = newId;
        if (tradeTargetId == oldId) tradeTargetId = newId;

        // Jury
        if (juryIds != null && juryIds.contains(oldId)) {
            juryIds.remove(oldId);
            juryIds.add(newId);
        }
        if (juryOrder != null) {
            Collections.replaceAll(juryOrder, oldId, newId);
        }
        if (juryVotes != null && juryVotes.containsKey(oldId)) {
            juryVotes.put(newId, juryVotes.remove(oldId));
        }
        if (juryVoteCount != null && juryVoteCount.containsKey(oldId)) {
            juryVoteCount.put(newId, juryVoteCount.remove(oldId));
        }
        if (bankruptEventUsedRound != null && bankruptEventUsedRound.containsKey(oldId)) {
            bankruptEventUsedRound.put(newId, bankruptEventUsedRound.remove(oldId));
        }
        if (themeContinuatorId == oldId) themeContinuatorId = newId;
    }
}

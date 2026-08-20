package com.memopoly.game.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Главная модель состояния матча: содержит список игроков, фазу игры, состояние аукционов и мем-баттлов, лог событий и баланс банка мемов.
 */
public class GameState {
    public HashMap<Integer, Integer> cellOwners;
    public HashMap<Integer, Boolean> cellMortgaged;
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
    public int currentPlayerIndex;
    public GamePhase currentPhase;
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
    public int battleSetupCellIndex;  // cell that triggered the battle setup
    public ArrayList<Integer> battleParticipants;
    public ArrayList<Integer> battleInvited;
    public HashMap<Integer, Boolean> battleAccepted;
    public int battleTimerSeconds;
    public BattlePhase battlePhase;
    public int battleBank;
    public ArrayList<Integer> battleVoters;
    public boolean isInAuction;
    public int auctionCellId;
    public HashMap<Integer, Integer> auctionBids;
    public int currentAuctionTime;
    public int auctionStarterPlayerId;
    public int auctionCurrentPlayerId;

    public int memeBankPlayerId = -1;

    public GameState() {
        this.cellOwners = new HashMap<>();
        this.cellMortgaged = new HashMap<>();
        this.players = new ArrayList<>();
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

        this.isInBattle = false;
        this.battleMemes = new ArrayList<>();
        this.battleParticipants = new ArrayList<>();
        this.battleInvited = new ArrayList<>();
        this.battleAccepted = new HashMap<>();
        this.votes = new HashMap<>();
        this.battleOwnerId = -1;
        this.battleVoters = new ArrayList<>();
        this.isInAuction = false;
        this.auctionBids = new HashMap<>();
        this.auctionCellId = -1;
        this.auctionStarterPlayerId = -1;
        this.auctionCurrentPlayerId = -1;
    }

    public Player getCurrentPlayer() {
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    public void nextPlayer() {
        int checkedPlayers = 0;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
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

        turnCount++;
        diceValue = 0;
        hasRolledThisTurn = false;
        memeBankPlayerId = -1;
        currentPhase = GamePhase.PLAYING;
        lastActionLog = "Ход переходит к " + getCurrentPlayer().name;
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
            if (p.id == id) return p;
        }
        return null;
    }

    public boolean isGameOver() {
        int activePlayers = 0;
        for (Player p : players) {
            if (!p.isBankrupt) activePlayers++;
        }
        return activePlayers <= 1;
    }

    public Player getWinner() {
        if (!isGameOver()) return null;

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

    public boolean containsMeme(int targetId){
        for(Meme i : battleMemes){
            if(i.id == targetId){
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
    public void endAuction(){
        isInAuction = false;
        auctionCellId = -1;
        auctionBids.clear();
        auctionStarterPlayerId = -1;
        auctionCurrentPlayerId = -1;
        currentAuctionTime = 30;
        currentPhase = GamePhase.PLAYING;
    }
}

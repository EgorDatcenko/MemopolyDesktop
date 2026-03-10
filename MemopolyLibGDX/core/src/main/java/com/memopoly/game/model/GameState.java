package com.memopoly.game.model;

import java.util.ArrayList;
import java.util.HashMap;

public class GameState {
    // Фазы игры
    public enum GamePhase {
        WAITING,       // Ожидание игроков
        PLAYING,       // Основная игра
        ROLLING_DICE,  // Бросок кубиков
        PLAYER_ACTION, // Выбор действия (купить/отказаться)
        MEME_BATTLE,   // Мем-баттл
        AUCTION,       // Аукцион
        GAME_OVER      // Конец игры
    }

    // Основные поля для KryoNet
    public ArrayList<Player> players;
    public ArrayList<BoardCell> board;
    public int currentPlayerIndex;
    public GamePhase currentPhase;
    public int diceValue;
    public String lastActionLog;
    public int turnCount;

    // Мем-баттл состояние
    public boolean isInBattle;
    public int battleStakes;
    public String battleTopic;
    public ArrayList<Meme> battleMemes;
    public HashMap<Integer, Integer> votes; // memeId -> voteCount
    public int battleOwnerId; // Для баттла на чужой клетке

    // Аукцион состояние
    public boolean isInAuction;
    public int auctionCellId;
    public HashMap<Integer, Integer> auctionBids; // playerId -> bid
    public int currentAuctionTime;

    // Стандартные конструкторы
    public GameState() {
        this.players = new ArrayList<>();
        this.board = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.currentPhase = GamePhase.WAITING;
        this.diceValue = 0;
        this.lastActionLog = "Игра началась";
        this.turnCount = 0;

        this.isInBattle = false;
        this.battleMemes = new ArrayList<>();
        this.votes = new HashMap<>();

        this.isInAuction = false;
        this.auctionBids = new HashMap<>();
    }

    // Игровые методы
    public Player getCurrentPlayer() {
        if (players.isEmpty()) return null;
        return players.get(currentPlayerIndex);
    }

    public void nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        turnCount++;
        lastActionLog = "Ход переходит к " + getCurrentPlayer().name;
    }

    public void addPlayer(Player player) {
        players.add(player);
        lastActionLog = "Игрок " + player.name + " присоединился";
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

    public BoardCell getCurrentCell() {
        Player current = getCurrentPlayer();
        if (current == null || board.isEmpty()) return null;
        return board.get(current.position);
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

    // Методы для мем-баттла
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

    public void endMemeBattle() {
        isInBattle = false;
        battleStakes = 0;
        battleTopic = "";
        battleOwnerId = -1;
        battleMemes.clear();
        votes.clear();
        currentPhase = GamePhase.PLAYING;
    }

    // Методы для аукциона
    public void startAuction(int cellId) {
        isInAuction = true;
        auctionCellId = cellId;
        auctionBids.clear();
        currentAuctionTime = 30; // 30 секунд
        currentPhase = GamePhase.AUCTION;
        lastActionLog = "Начинается аукцион!";
    }
}

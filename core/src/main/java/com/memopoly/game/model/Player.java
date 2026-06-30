package com.memopoly.game.model;

import java.util.ArrayList;

/**
 * Состояние игрока: хранит баланс денег, позицию на поле, статус банкротства, карты мемов в руке, купленные клетки и баланс в банке мемов.
 */
public class Player {
    public int id;
    public String name;
    public int money;
    public int position;
    public boolean isBankrupt;
    public ArrayList<Meme> handMemes;
    public ArrayList<Integer> ownedCells;
    public int maxAffordable;
    public int memeBankBalance;

    public Player(){
        this.handMemes = new ArrayList<>();
        this.ownedCells = new ArrayList<>();
    }

    public Player(int id, String name){
        this();
        this.id = id;
        this.name = name;
        this.money = 1500;
        this.position = 0;
        this.isBankrupt = false;
        this.memeBankBalance = 0;
    }

    public boolean canAfford(int amount){
        return maxAffordable >= amount;
    }

    public void pay(int amount) {
        if (maxAffordable < amount) {
            isBankrupt = true;
            money = 0;
            return;
        }
        money -= amount;
        if (money < 0) {
            money = 0;
        }
    }

    public boolean containsMeme(int targetId){
        for(Meme i : handMemes){
            if(i.id == targetId){
                return true;
            }
        }
        return false;
    }
    public void receive(int amount){
        money += amount;
    }
}

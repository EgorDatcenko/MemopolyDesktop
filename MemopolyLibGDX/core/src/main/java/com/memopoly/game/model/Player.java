package com.memopoly.game.model;

import java.util.ArrayList;

public class Player {
    public int id;
    public String name;
    public int money;
    public int position;
    public boolean isBankrupt;
    public ArrayList<Meme> handMemes;
    public ArrayList<Integer> ownedCells;

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
    }

    public boolean canAfford(int amount){
        return  money >= amount;
    }

    public void pay(int amount){
        money -= amount;
        if(money <= 0){
            isBankrupt = true;
        }
    }
    public void receive(int amount){
        money += amount;
    }
}

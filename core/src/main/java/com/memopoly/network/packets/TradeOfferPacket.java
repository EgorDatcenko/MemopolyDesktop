package com.memopoly.network.packets;

import java.util.ArrayList;

/**
 * Пакет предложения сделки: инициатор предлагает обмен клетками и монетами.
 */
public class TradeOfferPacket {
    public int targetId;                  // ID игрока, которому предлагается сделка
    public ArrayList<Integer> myCells;    // Клетки инициатора
    public ArrayList<Integer> theirCells; // Клетки цели
    public int myMoney;                   // Монеты инициатора
    public int theirMoney;                // Монеты цели
}

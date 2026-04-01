package com.memopoly.network;

import com.esotericsoftware.kryo.Kryo;
import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.EventCard;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Meme;
import com.memopoly.game.model.Player;
import com.memopoly.network.packets.ChatMessage;
import com.memopoly.network.packets.CreateRoomRequest;
import com.memopoly.network.packets.CreateRoomResponse;
import com.memopoly.network.packets.GameActionRequest;
import com.memopoly.network.packets.GameStatePacket;
import com.memopoly.network.packets.JoinRoomRequest;
import com.memopoly.network.packets.JoinRoomResponse;
import com.memopoly.network.packets.RollDiceRequest;
import com.memopoly.network.packets.RollDiceResponse;
import com.memopoly.network.packets.StartGameRequest;

import java.util.ArrayList;
import java.util.HashMap;

public class NetworkRegistry {
    public static void register(Kryo kryo) {
        kryo.setRegistrationRequired(true);
        kryo.setReferences(true);

        kryo.register(CreateRoomRequest.class);
        kryo.register(CreateRoomResponse.class);
        kryo.register(JoinRoomRequest.class);
        kryo.register(JoinRoomResponse.class);
        kryo.register(RollDiceRequest.class);
        kryo.register(RollDiceResponse.class);
        kryo.register(GameActionRequest.class);
        kryo.register(ChatMessage.class);
        kryo.register(GameStatePacket.class);
        kryo.register(StartGameRequest.class);

        kryo.register(GameActionRequest.ActionType.class);
        kryo.register(GameState.GamePhase.class);
        kryo.register(BoardCell.Type.class);
        kryo.register(BoardCell.Group.class);
        kryo.register(EventCard.EffectType.class);

        kryo.register(Player.class);
        kryo.register(BoardCell.class);
        kryo.register(GameState.class);
        kryo.register(Meme.class);
        kryo.register(EventCard.class);

        kryo.register(ArrayList.class);
        kryo.register(HashMap.class);
    }
}

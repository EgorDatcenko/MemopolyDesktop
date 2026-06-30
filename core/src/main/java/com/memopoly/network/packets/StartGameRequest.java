package com.memopoly.network.packets;

/**
 * Пакет запроса старта игры: отправляется хостом из лобби для перевода игры в фазу активного матча.
 */
public class StartGameRequest {
    public String deckName;

    public StartGameRequest() {
    }
}

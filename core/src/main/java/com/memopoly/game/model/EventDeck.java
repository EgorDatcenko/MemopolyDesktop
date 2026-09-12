package com.memopoly.game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EventDeck {
    private static final List<EventCard> CARDS = buildCards();

    public static EventCard drawRandom() {
        return CARDS.get(ThreadLocalRandom.current().nextInt(CARDS.size()));
    }

    private static List<EventCard> buildCards() {
        List<EventCard> cards = new ArrayList<>();
        cards.add(new EventCard(1, "Спонсорский контракт", "Получи 200, но пропусти следующий сбор ренты с одной клетки.", EventCard.EffectType.SKIP_NEXT_RENT_COLLECTION, 200));
        cards.add(new EventCard(2, "Хайповый тренд", "Получи по 20 монет за каждую твою клетку.", EventCard.EffectType.RECEIVE_PER_OWNED_CELL, 20));
        cards.add(new EventCard(3, "DMCA-страйк", "Заплати штраф по 15 монет за каждую твою клетку.", EventCard.EffectType.PAY_PER_OWNED_CELL, 15));
        cards.add(new EventCard(4, "Краудфандинг", "Все остальные игроки скидываются тебе по 30 монет.", EventCard.EffectType.COLLECT_FROM_ALL, 30));
        cards.add(EventCard.createSkipTurnCard(5, "Залип в ленте", "Пропусти следующий ход."));
        cards.add(new EventCard(6, "Интернет-виральность", "Сразу ещё раз брось кубики и сходи.", EventCard.EffectType.EXTRA_ROLL));
        cards.add(new EventCard(7, "Возврат на Старт", "Телепортируйся на Старт без получения бонуса.", EventCard.EffectType.RETURN_TO_START));
        cards.add(new EventCard(8, "Финансовый кризис", "Рынок рухнул! Весь твой вклад в Meme Bank сгорает.", EventCard.EffectType.MARKET_CRASH));
        cards.add(EventCard.createReceiveCard(9, "Вирусный пост", "Твой мем набрал миллион лайков. Получи 150.", 150));
        cards.add(EventCard.createPayCard(10, "Бан аккаунта", "Модератор удалил твой контент. Заплати 80.", 80));
        cards.add(new EventCard(11, "Партнёрская интеграция", "Реклама зашла аудитории. Получи 120.", EventCard.EffectType.RECEIVE_MONEY, 120));
        cards.add(new EventCard(12, "Платная верификация", "Синяя галочка оказалась дороже плана. Заплати 50.", EventCard.EffectType.PAY_MONEY, 50));
        cards.add(new EventCard(13, "Мемный грант", "Фонд цифрового искусства поддержал тебя. Получи 100.", EventCard.EffectType.RECEIVE_MONEY, 100));
        cards.add(new EventCard(14, "Ребрендинг канала", "Срочно меняешь стиль. Заплати 10 за каждую свою клетку.", EventCard.EffectType.PAY_PER_OWNED_CELL, 10));
        return cards;
    }
}

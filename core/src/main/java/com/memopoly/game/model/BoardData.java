package com.memopoly.game.model;

import java.util.ArrayList;
import java.util.List;

public class BoardData {
//    public static List<BoardCell> buildCells() {
//        List<BoardCell> cells = new ArrayList<>();
//
//        cells.add(new BoardCell(0,  BoardCell.Type.START,       "Старт"));
//        cells.add(new BoardCell(1,  "r/memes",              60,  BoardCell.Group.REDDIT));
//        cells.add(new BoardCell(2,  "r/dankmemes",          80,  BoardCell.Group.REDDIT));
//        cells.add(new BoardCell(3,  BoardCell.Type.EVENT,       "Событие"));
//        cells.add(new BoardCell(4,  "r/me_irl",             100, BoardCell.Group.REDDIT));
//        cells.add(new BoardCell(5,  BoardCell.Type.TAX,         "Налог"));
//        cells.add(new BoardCell(6,  "#челлендж",            120, BoardCell.Group.TIKTOK));
//        cells.add(new BoardCell(7,  "Тренд недели",         130, BoardCell.Group.TIKTOK));
//        cells.add(new BoardCell(8,  BoardCell.Type.EVENT,       "Событие"));
//        cells.add(new BoardCell(9,  "Озвучка мемов",        140, BoardCell.Group.TIKTOK));
//        cells.add(new BoardCell(10, BoardCell.Type.REST,        "Отдых"));
//        cells.add(new BoardCell(11, "Лента мемов",          160, BoardCell.Group.INSTAGRAM));
//        cells.add(new BoardCell(12, "Reels",                170, BoardCell.Group.INSTAGRAM));
//        cells.add(new BoardCell(13, BoardCell.Type.EVENT,       "Событие"));
//        cells.add(new BoardCell(14, "Stories",              180, BoardCell.Group.INSTAGRAM));
//        cells.add(new BoardCell(15, BoardCell.Type.MEME_BATTLE, "Meme Battle"));
//        cells.add(new BoardCell(16, "Твит недели",          200, BoardCell.Group.X));
//        cells.add(new BoardCell(17, "Цитатный ретвит",      210, BoardCell.Group.X));
//        cells.add(new BoardCell(18, BoardCell.Type.EVENT,       "Событие"));
//        cells.add(new BoardCell(19, "Мемный тред",          220, BoardCell.Group.X));
//        cells.add(new BoardCell(20, BoardCell.Type.REST,        "Свободная парковка"));
//        cells.add(new BoardCell(21, "Мемный канал",         240, BoardCell.Group.TELEGRAM));
//        cells.add(new BoardCell(22, "Стикерпак",            250, BoardCell.Group.TELEGRAM));
//        cells.add(new BoardCell(23, BoardCell.Type.MEME_BATTLE, "Meme Battle"));
//        cells.add(new BoardCell(24, "Чат мемоделов",        260, BoardCell.Group.TELEGRAM));
//        cells.add(new BoardCell(25, BoardCell.Type.MEME_BANK,   "Мем-банк"));
//        cells.add(new BoardCell(26, "Сервер мемов",         280, BoardCell.Group.DISCORD));
//        cells.add(new BoardCell(27, "Голосовой мем",        290, BoardCell.Group.DISCORD));
//        cells.add(new BoardCell(28, BoardCell.Type.EVENT,       "Событие"));
//        cells.add(new BoardCell(29, "Канал #мемы",          300, BoardCell.Group.DISCORD));
//        cells.add(new BoardCell(30, BoardCell.Type.JAIL,        "Нарушение копирайта"));
//        cells.add(new BoardCell(31, "Shorts",               320, BoardCell.Group.YOUTUBE));
//        cells.add(new BoardCell(32, "Комменты под видео",   330, BoardCell.Group.YOUTUBE));
//        cells.add(new BoardCell(33, BoardCell.Type.EVENT,       "Событие"));
//        cells.add(new BoardCell(34, "Мемная озвучка",       340, BoardCell.Group.YOUTUBE));
//        cells.add(new BoardCell(35, BoardCell.Type.MEME_BATTLE, "Meme Battle"));
//        cells.add(new BoardCell(36, "Момент со стрима",     350, BoardCell.Group.TWITCH));
//        cells.add(new BoardCell(37, BoardCell.Type.TAX,         "Налог на хайп"));
//        cells.add(new BoardCell(38, "Клип недели",          380, BoardCell.Group.TWITCH));
//        cells.add(new BoardCell(39, "Чат стрима",            400, BoardCell.Group.TWITCH));
//
//        return cells;
//    }
    public static List<BoardCell> buildCells() {
        List<BoardCell> cells = new ArrayList<>();

        cells.add(new BoardCell(0,  BoardCell.Type.START,       "Start"));
        cells.add(new BoardCell(1,  "r/memes",              60,  BoardCell.Group.REDDIT));
        cells.add(new BoardCell(2,  "r/dankmemes",          80,  BoardCell.Group.REDDIT));
        cells.add(new BoardCell(3,  BoardCell.Type.EVENT,       "Event"));
        cells.add(new BoardCell(4,  "r/me_irl",             100, BoardCell.Group.REDDIT));
        cells.add(new BoardCell(5, BoardCell.Type.MEME_BATTLE, "Meme Battle"));
        cells.add(new BoardCell(6,  "#challenge",           120, BoardCell.Group.TIKTOK));
        cells.add(new BoardCell(7,  "Trend of the week",    130, BoardCell.Group.TIKTOK));
        cells.add(new BoardCell(8,  BoardCell.Type.TAX,       "Tax"));
        cells.add(new BoardCell(9,  "Meme voiceover",       140, BoardCell.Group.TIKTOK));
        cells.add(new BoardCell(10, BoardCell.Type.REST,        "Rest"));
        cells.add(new BoardCell(11, "Meme feed",            160, BoardCell.Group.INSTAGRAM));
        cells.add(new BoardCell(12, "Reels",                170, BoardCell.Group.INSTAGRAM));
        cells.add(new BoardCell(13, BoardCell.Type.EVENT,       "Event"));
        cells.add(new BoardCell(14, "Stories",              180, BoardCell.Group.INSTAGRAM));
        cells.add(new BoardCell(15, BoardCell.Type.MEME_BATTLE, "Meme Battle"));
        cells.add(new BoardCell(16, "Tweet of the week",    200, BoardCell.Group.X));
        cells.add(new BoardCell(17, "Quote retweet",        210, BoardCell.Group.X));
        cells.add(new BoardCell(18, BoardCell.Type.EVENT,       "Event"));
        cells.add(new BoardCell(19, "Meme thread",          220, BoardCell.Group.X));
        cells.add(new BoardCell(20, BoardCell.Type.REST,        "Free parking"));
        cells.add(new BoardCell(21, "Meme channel",         240, BoardCell.Group.TELEGRAM));
        cells.add(new BoardCell(22, "Sticker pack",         250, BoardCell.Group.TELEGRAM));
        cells.add(new BoardCell(23, BoardCell.Type.MEME_BATTLE, "Meme Battle"));
        cells.add(new BoardCell(24, "Meme makers chat",     260, BoardCell.Group.TELEGRAM));
        cells.add(new BoardCell(25, BoardCell.Type.MEME_BANK,   "Meme Bank"));
        cells.add(new BoardCell(26, "Meme server",          280, BoardCell.Group.DISCORD));
        cells.add(new BoardCell(27, "Voice chat",           290, BoardCell.Group.DISCORD));
        cells.add(new BoardCell(28, BoardCell.Type.EVENT,       "Event"));
        cells.add(new BoardCell(29, "#memes channel",       300, BoardCell.Group.DISCORD));
        cells.add(new BoardCell(30, BoardCell.Type.JAIL,        "Copyright Infringement"));
        cells.add(new BoardCell(31, "Shorts",               320, BoardCell.Group.YOUTUBE));
        cells.add(new BoardCell(32, "Video comments",       330, BoardCell.Group.YOUTUBE));
        cells.add(new BoardCell(33, BoardCell.Type.EVENT,       "Event"));
        cells.add(new BoardCell(34, "Meme video",       340, BoardCell.Group.YOUTUBE));
        cells.add(new BoardCell(35, BoardCell.Type.MEME_BATTLE, "Meme Battle"));
        cells.add(new BoardCell(36, "Stream moment",        350, BoardCell.Group.TWITCH));
        cells.add(new BoardCell(37, BoardCell.Type.TAX,         "Hype tax"));
        cells.add(new BoardCell(38, "Clip of the week",     380, BoardCell.Group.TWITCH));
        cells.add(new BoardCell(39, "Stream chat",            400, BoardCell.Group.TWITCH));

        return cells;
    }
}

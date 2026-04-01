# 🎮 ROADMAP: Мемополия — Desktop-версия на Java + LibGDX + KryoNet

> Этот документ описывает план переноса текущей WEB-версии «Мемополии» (Node.js + Socket.IO + SQLite + vanilla HTML/JS) на десктопную архитектуру с новым технологическим стеком.  
> Все игровые механики сохранены согласно `instruction.md`.

---

## 📋 Оглавление

1. [Анализ текущего WEB-проекта](#1-анализ-текущего-web-проекта)
2. [Новая архитектура](#2-новая-архитектура)
3. [Технологический стек](#3-технологический-стек)
4. [Структура проекта](#4-структура-проекта)
5. [Этапы разработки (Roadmap)](#5-этапы-разработки-roadmap)
6. [Перенос игровой логики](#6-перенос-игровой-логики)
7. [Источники и ссылки для изучения](#7-источники-и-ссылки-для-изучения)

---

## 1. Анализ текущего WEB-проекта

### Что реализовано в WEB-версии

| Модуль | Файл | Описание |
|--------|------|----------|
| HTTP-сервер | `index.js` | Express 5, serve static, REST-эндпоинты |
| WebSocket | `index.js` | Socket.IO 4 — все события игры |
| Аутентификация | `index.js` | Passport.js + Google OAuth 2.0 |
| Игровая логика | `src/gameLogic.js` | ~1000 строк: поле, мем-баттл, аукцион, ход |
| БД | `src/db/schema.js` | SQLite (better-sqlite3): users, memes, decks, event_cards, situations |
| Фронтенд | `public/index.html` | Один SPA-файл, ванильный JS |
| Клиентская логика | `public/js/game.js` | Рендер, сокет-события, интерфейс |

### Ключевые Socket.IO события (→ KryoNet-сообщения)

| Socket событие | Действие | Java-аналог |
|----------------|----------|-------------|
| `create_room` | Создать комнату | `CreateRoomRequest` |
| `join_room` | Зайти по ID | `JoinRoomRequest` |
| `join_room_by_code` | Зайти по коду | `JoinByCodeRequest` |
| `start_game` | Начать (host) | `StartGameRequest` |
| `roll_dice` | Бросить кубики | `RollDiceRequest` |
| `game_action` | Мультиплексор действий | `GameActionRequest` с полем `actionType` |
| `game_state_updated` | Синхронизация | `GameStateMessage` |
| `mem_battle_*` | Все события баттла | `MemBattleMessage` |

### Проблемы WEB-версии, которые решает Desktop

- Требует постоянного сервера (даже у localhost).
- Нет нативных анимаций, звука, шрифтов вне браузера.
- Google OAuth — лишняя зависимость для игры «у друга».
- Большой монолитный `index.js` (~1550 строк) без разделения ответственности.

---

## 2. Новая архитектура

```
┌─────────────────────────────────────────────────────────┐
│                  Listen Server (хост)                    │
│  ┌──────────────────────────────────────────────────┐   │
│  │          KryoNet Server (отдельный поток)         │   │
│  │  - принимает подключения клиентов                 │   │
│  │  - рассылает GameStatePacket всем                 │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────┐   │
│  │          GameEngine (основной поток LibGDX)       │   │
│  │  - GameState (доска, игроки, фазы, баттл)        │   │
│  │  - MemBattleManager, AuctionManager               │   │
│  │  - BoardRenderer, UIRenderer                      │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────┐   │
│  │          MemeDatabase (JSON / SQLite-lite)        │   │
│  │  - загрузка мемов, ситуаций, карт событий        │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
         ▲ UDP/TCP (KryoNet)    │
         │                     ▼
┌────────────────┐    ┌────────────────┐    ┌────────────────┐
│  Client #1     │    │  Client #2     │    │  Client #N     │
│  LibGDX App    │    │  LibGDX App    │    │  LibGDX App    │
│  - Screens     │    │  - Screens     │    │  - Screens     │
│  - KryoClient  │    │  - KryoClient  │    │  - KryoClient  │
└────────────────┘    └────────────────┘    └────────────────┘
```

**Паттерн Listen Server**: хост запускает и сервер (GameServer), и клиент (GameClient) в одном процессе. Гости запускают только клиент.

---

## 3. Технологический стек

### Основной стек

| Компонент | Технология | Версия (последняя стабильная) | Обоснование |
|-----------|-----------|-------------------------------|-------------|
| Язык | Java | **21 LTS** | Долгосрочная поддержка, pattern matching, records |
| Игровой фреймворк | LibGDX | **1.13.1** | 2D-рендер, аудио, Scene2D UI, кроссплатформа |
| Сеть | KryoNet | **2.22.9-RC1** (или форк `gdx-kryonet`) | Автосериализация, TCP+UDP, минимум кода |
| Сборка | Gradle | **8.10+** (через gdx-setup или вручную) | Стандарт LibGDX |
| UI-библиотека | Scene2D (входит в LibGDX) | встроена | Виджеты, сцены, таблицы, диалоги |
| Форматы данных | JSON (libgdx-json) | встроена | Сохранение настроек, колоды мемов |
| Опционально: БД | H2 Database (embedded) | **2.3.232** | Если нужен полноценный SQL на хосте |
| Логирование | SLF4J + Logback | **2.0.16 / 1.5.12** | Структурированные логи |

### Опциональные библиотеки

| Библиотека | Назначение |
|-----------|-----------|
| **VisUI** (1.5.3) | Расширенные Scene2D-виджеты, темная тема |
| **Ashley ECS** (1.7.4) | Entity-Component-System (необязательно для этой игры) |
| **gdx-audio** | Продвинутые звуковые эффекты |
| **Steamworks4J** (1.9.0) | Интеграция Steam (будущая задача) |
| **Radmin VPN** | Virtual LAN для тестирования без проброса портов |

---

## 4. Структура проекта

```
memopoly/
├── build.gradle          ← корневой
├── settings.gradle
├── gradle.properties     ← версии зависимостей
│
├── core/                 ← общая логика (LibGDX Core)
│   └── src/main/java/com/memopoly/
│       ├── MemopoLy.java             ← точка входа (LibGDX Game)
│       ├── Screens/
│       │   ├── MainMenuScreen.java
│       │   ├── LobbyScreen.java
│       │   ├── GameScreen.java
│       │   └── ResultScreen.java
│       ├── game/
│       │   ├── model/
│       │   │   ├── GameState.java    ← главное состояние
│       │   │   ├── Player.java
│       │   │   ├── BoardCell.java
│       │   │   ├── Meme.java
│       │   │   ├── EventCard.java
│       │   │   └── Situation.java
│       │   ├── logic/
│       │   │   ├── GameEngine.java   ← основной двигатель
│       │   │   ├── MemBattleManager.java
│       │   │   ├── AuctionManager.java
│       │   │   ├── BoardFactory.java ← создание 40-клеточного поля
│       │   │   └── BotPlayer.java
│       │   └── BoardRenderer.java
│       ├── network/
│       │   ├── packets/              ← все KryoNet-пакеты (POJO)
│       │   │   ├── GameStatePacket.java
│       │   │   ├── RollDiceRequest.java
│       │   │   ├── GameActionRequest.java
│       │   │   └── ...
│       │   ├── GameServer.java       ← KryoNet Server
│       │   ├── GameClient.java       ← KryoNet Client
│       │   └── NetworkRegistry.java  ← регистрация всех классов
│       ├── data/
│       │   ├── MemeDatabase.java     ← загрузка мемов
│       │   └── DeckManager.java      ← колоды
│       └── ui/
│           ├── HUD.java
│           ├── MemBattleDialog.java
│           ├── AuctionDialog.java
│           └── EventCardDialog.java
│
├── desktop/              ← запускатор для Desktop
│   └── src/main/java/com/memopoly/desktop/
│       └── DesktopLauncher.java
│
└── assets/               ← ресурсы (мемы, атласы, шрифты, звуки)
    ├── memes/
    ├── skins/
    ├── data/
    │   ├── memes.json
    │   ├── situations.json
    │   └── event_cards.json
    └── audio/
```

---

## 5. Этапы разработки (Roadmap)

### 🟦 Фаза 0 — Настройка проекта (1–2 дня)

- [ ] Установить Java 21 JDK (Eclipse Temurin / Adoptium)
- [ ] Установить IntelliJ IDEA Community или Ultimate
- [ ] Создать проект через [gdx-setup](https://libgdx.com/wiki/start/project-generation) или вручную с Gradle
- [ ] Подключить зависимость KryoNet в `build.gradle`
- [ ] Добавить VisUI в зависимости (тема сразу)
- [ ] Настроить `DesktopLauncher` с нужным разрешением (1280×800)
- [ ] Запустить пустой проект, убедиться, что окно открывается

**Источник:** [LibGDX Wiki — Setting up a Project](https://libgdx.com/wiki/start/project-generation)

---

### 🟦 Фаза 1 — Модели данных (2–3 дня)

- [ ] Перенести все игровые объекты JS → Java POJO:
  - `Player`, `BoardCell`, `GameState`, `Meme`, `Situation`, `EventCard`
  - `MemBattleState`, `AuctionState`
- [ ] Создать `BoardFactory.java` — генерация 40 клеток (аналог `createBoard()`)
- [ ] Создать JSON-файлы ресурсов: `memes.json`, `situations.json`, `event_cards.json`
- [ ] Написать `MemeDatabase.java` — загрузка мемов из JSON через `libgdx.utils.Json`
- [ ] Написать unit-тесты для моделей (JUnit 5)

**Аналог в JS:** `createBoard()`, `createPlayer()`, `createGameState()` в `src/gameLogic.js`

---

### 🟦 Фаза 2 — Сетевой слой (3–4 дня)

- [ ] Создать все Packet-классы (POJO без конструкторов с аргументами — требование KryoNet)
- [ ] Написать `NetworkRegistry.java` — регистрация всех классов в Kryo
- [ ] Реализовать `GameServer.java`:
  - Принимать входящие соединения
  - Хранить `Map<Connection, Player>`
  - Обрабатывать `CreateRoomRequest`, `JoinRoomRequest`, `StartGameRequest`
  - Рассылать `GameStatePacket` всем после каждого изменения
- [ ] Реализовать `GameClient.java`:
  - Асинхронное подключение к хосту
  - Callback-интерфейс `NetworkListener` → уведомляет `GameScreen`
- [ ] Тест: два экземпляра приложения соединяются по localhost

**Аналог в JS:** весь `io.on('connection', ...)` блок в `index.js`

---

### 🟦 Фаза 3 — Игровой движок (5–7 дней)

- [ ] Реализовать `GameEngine.java`:
  - Метод `rollDice()` → `movePlayer()` → `handleCellAction()`
  - Хранить `currentPlayerIndex`, фазы (`rolling`, `action`, `mem-battle`, `auction`)
  - Метод `nextPlayer()`
- [ ] Реализовать `BoardFactory.java` с правильным расположением групп и цветов
- [ ] Реализовать экономику:
  - Покупка клетки
  - Налог
  - Старт (+200)
  - Заклад / выкуп (`mortgageCell`, `buybackCell`)
- [ ] Реализовать `AuctionManager.java`:
  - Аукцион при отказе от покупки (аналог `startAuction()`)
  - Ставки с таймером 30 сек
  - Принудительная покупка при отсутствии ставок
- [ ] Реализовать карты событий (`EventCard`) и их эффекты

**Аналог в JS:** `movePlayer()`, `handleCellAction()`, `startAuction()`, `finalizeAuction()` в `gameLogic.js`

---

### 🟦 Фаза 4 — Мем-баттл (5–7 дней)

- [ ] Реализовать `MemBattleManager.java`:
  - `startMemBattle()` — инициализация баттла (ситуация или Meme Battle клетка)
  - `startBettingBattle()` — баттл с ставками (Meme Battle клетка)
  - `setBetAndInvites()`, `acceptInvite()`, `declineInvite()`
  - `submitMeme()` — подача мема, сборка `shuffledMemes`
  - `castVote()` — голосование
  - `calculateBattleResults()` — подсчёт: победитель, ничья, re-vote
- [ ] Реализовать бот-логику (`BotPlayer.java`):
  - При 2 реальных игроках добавлять анонимного бота
  - Бот выбирает случайный мем
  - Голоса за бота исключаются из подсчёта

**Аналог в JS:** `startMemBattle()`, `submitMeme()`, `castVote()`, `calculateBattleResults()` в `gameLogic.js`

---

### 🟦 Фаза 5 — Экраны и UI (7–10 дней)

- [ ] **MainMenuScreen**: кнопки «Создать игру», «Подключиться», поле IP/порта
- [ ] **LobbyScreen**: список игроков, готовность, кнопка Start (только хост)
- [ ] **GameScreen** — основной класс с:
  - `BoardRenderer` — рисует 40 клеток по кругу (или квадратом, как в Монополии)
  - Панель слева — игроки, баланс, мемы
  - Кнопка кубиков
  - Лог событий
- [ ] Диалоги (Scene2D `Dialog`):
  - `BuyPropertyDialog` — купить или отказаться
  - `AuctionDialog` — таймер + ставка
  - `MemBattleTopicDialog` — ввод/рандом темы
  - `InviteDialog` — Да/Нет с таймером 30 сек
  - `MemeSelectionDialog` — выбор мема из руки (60 сек)
  - `VotingDialog` — анонимные мемы, голосование (30 сек)
  - `BattleResultDialog` — победитель, выплаты
  - `EventCardDialog` — показ карты события
- [ ] Таймеры в диалогах через `Timer` (LibGDX `com.badlogic.gdx.utils.Timer`)
- [ ] **ResultScreen**: итоги игры, победитель, статистика

---

### 🟦 Фаза 6 — Ресурсы и визуал (3–5 дней)

- [ ] Подобрать/создать атлас текстур (`TexturePacker`): иконки соцсетей, токены, кубики
- [ ] Подключить TTF-шрифт (через `FreeTypeFontGenerator`)
- [ ] Настроить VisUI-скин или создать собственный `skin.json` + текстуры
- [ ] Добавить звуки: бросок кубиков, монеты, победа, баттл
- [ ] Анимации токена (движение по клеткам с интерполяцией)
- [ ] Встроить картинки мемов (Image из LibGDX или загрузка с диска/URL)

---

### 🟦 Фаза 7 — Колоды и данные (2–3 дня)

- [ ] JSON-формат колоды: `{ "id": 1, "name": "...", "memes": [{ "id": 1, "url": "...", "desc": "..." }] }`
- [ ] `DeckManager.java` — загрузка/сохранение пользовательских колод
- [ ] Экран управления колодами (аналог вкладки «Колоды» в веб-версии)
- [ ] Опционально: замена JSON на H2 embedded DB для хранения данных

---

### 🟦 Фаза 8 — Тестирование сети (3–4 дня)

- [ ] Тест в Radmin VPN: хост создаёт игру, гости подключаются по VPN-IP
- [ ] Проверить все сценарии: покупка, налог, мем-баттл, аукцион
- [ ] Тест с 2 игроками (бот-мем должен появляться)
- [ ] Тест переподключения (клиент закрыл / переоткрыл окно)
- [ ] Балансировка: цены, ставки, выплаты — согласно `instruction.md`

---

### 🟦 Фаза 9 — Полировка (2–3 дня)

- [ ] Сглаженные анимации движения фишки
- [ ] Звуковые эффекты
- [ ] Обработка крайних случаев: банкрот в середине баттла, отключение хоста
- [ ] Настройки (volume, fullscreen)
- [ ] Сборка `.jar` и `.exe` (через `launch4j` или Gradle Shadow)

---

### 🟦 Фаза 10 — Steam-версия (опционально)

- [ ] Подключить `Steamworks4J`
- [ ] Использовать Steam Lobby вместо ручного IP
- [ ] Добавить Steam Rich Presence
- [ ] Подготовить страницу в Steam ($100 взнос)

---

## 6. Перенос игровой логики

### Таблица соответствий JS → Java

| JS-функция (gameLogic.js) | Java-метод | Класс |
|---------------------------|------------|-------|
| `createBoard()` | `BoardFactory.create()` | `BoardFactory` |
| `createPlayer()` | `new Player(...)` | `Player` |
| `createGameState()` | `GameEngine.initGame()` | `GameEngine` |
| `drawMemes()` | `DeckManager.draw(int count)` | `DeckManager` |
| `rollDice()` | `GameEngine.rollDice()` | `GameEngine` |
| `movePlayer()` | `GameEngine.movePlayer(Player, int steps)` | `GameEngine` |
| `startMemBattle()` | `MemBattleManager.startBattle(...)` | `MemBattleManager` |
| `submitMeme()` | `MemBattleManager.submitMeme(Player, Meme)` | `MemBattleManager` |
| `castVote()` | `MemBattleManager.castVote(Player, int memeId)` | `MemBattleManager` |
| `calculateBattleResults()` | `MemBattleManager.calculateResults()` | `MemBattleManager` |
| `startAuction()` | `AuctionManager.startAuction(BoardCell)` | `AuctionManager` |
| `placeBid()` | `AuctionManager.placeBid(Player, int amount)` | `AuctionManager` |
| `finalizeAuction()` | `AuctionManager.finalize()` | `AuctionManager` |
| `nextPlayer()` | `GameEngine.nextTurn()` | `GameEngine` |
| `checkWinCondition()` | `GameEngine.checkWin()` | `GameEngine` |
| `addLog()` | `GameState.addLog(String)` | `GameState` |

### Важные особенности KryoNet

1. **Все Packet-классы должны иметь публичный конструктор без аргументов.**
2. Регистрировать нужно **и на сервере, и на клиенте** через `NetworkRegistry.register(Kryo)`.
3. `ArrayList`, `HashMap` и другие коллекции тоже нужно регистрировать в Kryo.
4. Состояние игры (`GameState`) должно быть **сериализуемым** Kryo — избегать лямбд и transient-полей.
5. Обновления состояния слать как `GameStatePacket` (весь объект) или дельта-пакетами.

---

## 7. Источники и ссылки для изучения

### 📚 LibGDX

| Ресурс | Ссылка |
|--------|--------|
| Официальная документация | https://libgdx.com/wiki/ |
| Генератор проекта gdx-setup | https://libgdx.com/wiki/start/project-generation |
| Scene2D UI (таблицы, диалоги) | https://libgdx.com/wiki/graphics/2d/scene2d/scene2d-ui |
| TexturePacker (атлас спрайтов) | https://libgdx.com/wiki/tools/texture-packer |
| FreeType шрифты | https://libgdx.com/wiki/extensions/freetype |
| LibGDX Timer | https://libgdx.com/wiki/utils/timer |
| LibGDX GitHub | https://github.com/libgdx/libgdx |
| Unofficial LibGDX Guide (Tomb of Knowledge) | https://github.com/raeleus/guides |
| Курс LibGDX на YouTube (Brent Aureli) | https://www.youtube.com/playlist?list=PL6gx4X24PoP_HUOWoW_6dY3c3WBPASeMy |
| LibGDX Game Examples | https://github.com/libgdx/libgdx/tree/master/tests/gdx-tests |

### 📚 KryoNet

| Ресурс | Ссылка |
|--------|--------|
| Официальный репозиторий | https://github.com/EsotericSoftware/kryonet |
| Документация Kryo (сериализация) | https://github.com/EsotericSoftware/kryo |
| KryoNet Listen Server Tutorial | https://github.com/EsotericSoftware/kryonet#server |
| Регистрация классов в Kryo | https://github.com/EsotericSoftware/kryo#registration |
| Пример многопользовательской игры с KryoNet | https://github.com/code-disaster/steamworks4j/tree/master/java-steamworks |

### 📚 Java 21

| Ресурс | Ссылка |
|--------|--------|
| Adoptium JDK (Eclipse Temurin) | https://adoptium.net/ |
| Java Records (замена DTO-POJO) | https://docs.oracle.com/en/java/docs/books/jls/21/ |
| JUnit 5 (тестирование) | https://junit.org/junit5/docs/current/user-guide/ |
| Gradle User Guide | https://docs.gradle.org/current/userguide/ |

### 📚 VisUI (расширенные виджеты)

| Ресурс | Ссылка |
|--------|--------|
| Репозиторий VisUI | https://github.com/kotcrab/vis-ui |
| Документация VisUI | https://github.com/kotcrab/vis-ui/wiki |
| Готовые скины | https://github.com/kotcrab/vis-ui/tree/master/ui/src/main/resources |

### 📚 Radmin VPN (сеть без проброса портов)

| Ресурс | Ссылка |
|--------|--------|
| Скачать Radmin VPN | https://www.radmin-vpn.com/ |
| Руководство по настройке | https://help.radmin-vpn.com/getting-started/ |

### 📚 Steamworks4J (будущая задача)

| Ресурс | Ссылка |
|--------|--------|
| Репозиторий Steamworks4J | https://github.com/code-disaster/steamworks4j |
| Пример Lobby (P2P) | https://github.com/code-disaster/steamworks4j-test |
| Инструкция Steamworks | https://partner.steamgames.com/doc/sdk |

### 📚 Полезные Youtube-каналы для геймдев на Java

| Канал | Что смотреть |
|-------|-------------|
| **Brent Aureli Code** | Полный курс LibGDX (Level Up Your Code) |
| **Let's Build a Game** | Пошаговые LibGDX-проекты |
| **GamesFromScratch** | Обзоры движков, в том числе LibGDX |
| **Pedro's LibGDX Game** (GitHub) | Референсный проект Platformer |

### 📚 Примеры готовых игр на LibGDX + KryoNet (для изучения)

| Проект | Ссылка |
|--------|--------|
| SuperKoalio (LibGDX demo) | https://github.com/libgdx/libgdx/tree/master/tests/gdx-tests/src/com/badlogic/gdx/tests |
| Multiplayer Space Shooter (Kryo) | https://github.com/EsotericSoftware/kryonet/tree/master/test |
| Board game LibGDX template | https://github.com/rafaskb/gdx-gameservices |

---

## 🗓️ Ориентировочные сроки

| Фаза | Задача | Дней |
|------|--------|------|
| 0 | Настройка проекта | 1–2 |
| 1 | Модели данных | 2–3 |
| 2 | Сетевой слой (KryoNet) | 3–4 |
| 3 | Игровой движок | 5–7 |
| 4 | Мем-баттл | 5–7 |
| 5 | UI и экраны | 7–10 |
| 6 | Ресурсы и визуал | 3–5 |
| 7 | Колоды и данные | 2–3 |
| 8 | Тестирование сети | 3–4 |
| 9 | Полировка | 2–3 |
| **Итого** | | **~35–50 дней** |

> Оценка для одного разработчика, работающего 3–5 часов в день.

---

## ⚠️ Критически важные замечания

1. **KryoNet не thread-safe**: все изменения `GameState` должны происходить в одном потоке (либо использовать `Gdx.app.postRunnable()`).
2. **LibGDX не позволяет трогать GL-контекст вне GL-потока**: сетевые колбэки кладём в очередь через `Gdx.app.postRunnable()`.
3. **Kryo регистрация**: каждый класс, передаваемый по сети, ДОЛЖЕН быть зарегистрирован с одинаковым ID и на сервере, и на клиенте.
4. **Мемы-картинки**: в десктоп-версии лучше хранить мемы локально или загружать по URL через `AssetManager` с кастомным `TextureLoader`.
5. **Аутентификация**: в десктоп-версии Google OAuth не нужен — достаточно ввода имени в лобби. В будущем — Steam ID.

---

*Документ сформирован на основе анализа текущего WEB-проекта Мемополии (Node.js + Socket.IO + SQLite) и технического задания из `instruction.md`.*  
*Дата: 2026-03-02*

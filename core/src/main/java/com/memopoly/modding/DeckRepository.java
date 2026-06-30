package com.memopoly.modding;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.memopoly.game.model.Meme;
import com.memopoly.game.model.MemeDeck;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Репозиторий колод: отвечает за сохранение, загрузку и управление пользовательскими колодами мемов (моддинг).
 */
public class DeckRepository {
    private static final String STORAGE_PATH = "modding/decks.json";
    private static final String DECK_IMAGES_DIR = "modding/decks";
    private static final int STORAGE_VERSION = 1;
    private final Json json = new Json();

    public static class DeckStorage {
        public int version = STORAGE_VERSION;
        public Array<MemeDeck> decks = new Array<>();
    }

    public Array<MemeDeck> loadDecks() {
        FileHandle file = Gdx.files.local(STORAGE_PATH);
        if (!file.exists()) {
            return new Array<>();
        }
        String raw = file.readString("UTF-8");
        if (raw == null || raw.isBlank()) {
            return new Array<>();
        }

        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            Array<MemeDeck> decks = json.fromJson(Array.class, MemeDeck.class, raw);
            return decks == null ? new Array<>() : decks;
        }

        DeckStorage storage = json.fromJson(DeckStorage.class, raw);
        if (storage == null || storage.decks == null) {
            return new Array<>();
        }
        return storage.decks;
    }

    public void saveDecks(Array<MemeDeck> decks) {
        FileHandle file = Gdx.files.local(STORAGE_PATH);
        file.parent().mkdirs();
        DeckStorage storage = new DeckStorage();
        storage.decks = decks == null ? new Array<>() : decks;
        file.writeString(json.prettyPrint(storage), false, "UTF-8");
    }

    public MemeDeck createDeck(String deckName, Array<String> imagePaths) {
        String deckDirName = sanitizeFileName(deckName) + "_" + ThreadLocalRandom.current().nextInt(1000, 9999);
        MemeDeck deck = new MemeDeck(deckName);
        int imageIndex = 0;
        for (String imagePath : imagePaths) {
            Meme meme = new Meme();
            meme.id = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
            meme.deckName = deckName;
            meme.imageUrl = copyImageToLocalDeckFolder(imagePath, deckDirName, imageIndex++);
            meme.description = "";
            deck.memes.add(meme);
        }
        Array<MemeDeck> decks = loadDecks();
        decks.add(deck);
        saveDecks(decks);
        return deck;
    }

    private String copyImageToLocalDeckFolder(String sourcePath, String deckDirName, int imageIndex) {
        try {
            FileHandle source = Gdx.files.absolute(sourcePath);
            if (!source.exists()) {
                return sourcePath;
            }

            String extension = source.extension();
            String normalizedExtension = extension == null || extension.isBlank() ? "png" : extension.toLowerCase();
            FileHandle deckDir = Gdx.files.local(DECK_IMAGES_DIR + "/" + deckDirName);
            deckDir.mkdirs();
            FileHandle target = Gdx.files.local(deckDir.path() + "/meme_" + imageIndex + "." + normalizedExtension);

            Files.copy(source.file().toPath(), target.file().toPath(), StandardCopyOption.REPLACE_EXISTING);
            return target.path();
        } catch (Exception e) {
            return sourcePath;
        }
    }

    private String sanitizeFileName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "deck";
        }
        return raw.trim().replaceAll("[^a-zA-Z0-9._-]+", "_");
    }
}

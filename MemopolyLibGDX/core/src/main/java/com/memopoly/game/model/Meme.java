package com.memopoly.game.model;

public class Meme {
    public int id;
    public String imageUrl;
    public String description;
    public String deckName;

    public Meme(){}

    public Meme(int id, String imageUrl, String description){
        this();
        this.id = id;
        this.imageUrl = imageUrl;
        this.description = description;
        this.deckName = "default";
    }

    public Meme(int id, String imageUrl, String description, String deckName){
        this(id, imageUrl, description);
        this.deckName = deckName;
    }
}

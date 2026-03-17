package com.memopoly.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

public class Assets {
    public static final String ATLAS_UI = "ui/ui.atlas";
    public static final String ATLAS_MEMES = "memes/memes.atlas";

    private final AssetManager manager;

    public Assets(){
        manager = new AssetManager();
    }

    public void loadAll(){
        manager.load(ATLAS_UI, TextureAtlas.class);
        manager.load(ATLAS_MEMES, TextureAtlas.class);
    }

    public float update(){
        manager.update();
        return manager.getProgress();
    }

    public boolean isLoaded(){
        return manager.isFinished();
    }

    public TextureAtlas getUiAtlas(){
        return manager.get(ATLAS_UI, TextureAtlas.class);
    }

    public TextureAtlas getMemesAtlas(){
        return manager.get(ATLAS_MEMES, TextureAtlas.class);
    }

    public void dispose(){
        manager.dispose();
    }
}

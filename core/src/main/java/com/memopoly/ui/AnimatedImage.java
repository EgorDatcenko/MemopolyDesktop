package com.memopoly.ui;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Scene2D виджет для отображения анимированных GIF и обычных картинок.
 * Без аллокаций памяти в act(delta).
 */
public class AnimatedImage extends Image {
    private Animation<TextureRegion> animation;
    private float stateTime = 0f;
    private long lastFrameId = -1;

    public AnimatedImage(Animation<TextureRegion> animation) {
        super(animation != null ? new TextureRegionDrawable(animation.getKeyFrame(0)) : null);
        this.animation = animation;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (animation == null)
            return;
        long currentFrameId = com.badlogic.gdx.Gdx.graphics != null ? com.badlogic.gdx.Gdx.graphics.getFrameId() : -1;
        if (currentFrameId >= 0 && currentFrameId == lastFrameId) {
            return;
        }
        lastFrameId = currentFrameId;
        stateTime += delta;
        TextureRegion currentFrame = animation.getKeyFrame(stateTime, true);
        if (getDrawable() instanceof TextureRegionDrawable) {
            ((TextureRegionDrawable) getDrawable()).setRegion(currentFrame);
        } else {
            setDrawable(new TextureRegionDrawable(currentFrame));
        }
    }

    public void setAnimation(Animation<TextureRegion> animation) {
        this.animation = animation;
        this.stateTime = 0f;
        if (animation != null) {
            TextureRegion firstFrame = animation.getKeyFrame(0);
            if (getDrawable() instanceof TextureRegionDrawable) {
                ((TextureRegionDrawable) getDrawable()).setRegion(firstFrame);
            } else {
                setDrawable(new TextureRegionDrawable(firstFrame));
            }
        } else {
            setDrawable(null);
        }
    }

    public Animation<TextureRegion> getAnimation() {
        return animation;
    }
}

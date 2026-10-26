package solim.graphics;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Disposable;
import solim.core.DisposableOwner;

public class BlurDrawable implements Drawable, DisposableOwner {

    private float blurRadius;
    private @Nullable Element owner;
    private @Nullable Drawable baseDrawable;
    private @Nullable Color tint;
    private boolean disposed = false;
    private final List<Disposable> bindings = new ArrayList<>();

    private float leftWidth;
    private float rightWidth;
    private float topHeight;
    private float bottomHeight;
    private float minWidth;
    private float minHeight;

    public BlurDrawable(float blurRadius) {
        this(null, blurRadius);
    }

    public BlurDrawable(@Nullable Element owner, float blurRadius) {
        this.owner = owner;
        this.blurRadius = Math.max(0.1f, blurRadius);
    }

    public static BlurDrawable of(float blurRadius) {
        return new BlurDrawable(blurRadius);
    }

    public static BlurDrawable of(@Nullable Element owner, float blurRadius) {
        return new BlurDrawable(owner, blurRadius);
    }

    public @Nullable Element getOwner() {
        return owner;
    }

    public BlurDrawable owner(@Nullable Element owner) {
        this.owner = owner;
        return this;
    }

    public float getBlurRadius() {
        return blurRadius;
    }

    public BlurDrawable blurRadius(float blurRadius) {
        this.blurRadius = Math.max(0.1f, blurRadius);
        return this;
    }

    public @Nullable Drawable getBaseDrawable() {
        return baseDrawable;
    }

    public BlurDrawable baseDrawable(@Nullable Drawable baseDrawable) {
        this.baseDrawable = baseDrawable;
        if (baseDrawable != null) {
            this.leftWidth = baseDrawable.getLeftWidth();
            this.rightWidth = baseDrawable.getRightWidth();
            this.topHeight = baseDrawable.getTopHeight();
            this.bottomHeight = baseDrawable.getBottomHeight();
            this.minWidth = baseDrawable.getMinWidth();
            this.minHeight = baseDrawable.getMinHeight();
        }
        return this;
    }

    public @Nullable Color getTint() {
        return tint;
    }

    public BlurDrawable tint(@Nullable Color tint) {
        this.tint = tint;
        return this;
    }

    @Override
    public void draw(float x, float y, float width, float height) {
        Color current = Draw.getColor();
        float alpha = current.a;
        if (owner != null) {
            BlurRenderer.get().drawBackdropBlur(owner, x, y, width, height, blurRadius, alpha);
        }
        if (baseDrawable != null) {
            baseDrawable.draw(x, y, width, height);
        }
    }

    @Override
    public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        draw(x, y, width, height);
    }

    @Override
    public float getLeftWidth() {
        return baseDrawable != null ? baseDrawable.getLeftWidth() : leftWidth;
    }

    @Override
    public void setLeftWidth(float leftWidth) {
        this.leftWidth = leftWidth;
        if (baseDrawable != null) {
            baseDrawable.setLeftWidth(leftWidth);
        }
    }

    @Override
    public float getRightWidth() {
        return baseDrawable != null ? baseDrawable.getRightWidth() : rightWidth;
    }

    @Override
    public void setRightWidth(float rightWidth) {
        this.rightWidth = rightWidth;
        if (baseDrawable != null) {
            baseDrawable.setRightWidth(rightWidth);
        }
    }

    @Override
    public float getTopHeight() {
        return baseDrawable != null ? baseDrawable.getTopHeight() : topHeight;
    }

    @Override
    public void setTopHeight(float topHeight) {
        this.topHeight = topHeight;
        if (baseDrawable != null) {
            baseDrawable.setTopHeight(topHeight);
        }
    }

    @Override
    public float getBottomHeight() {
        return baseDrawable != null ? baseDrawable.getBottomHeight() : bottomHeight;
    }

    @Override
    public void setBottomHeight(float bottomHeight) {
        this.bottomHeight = bottomHeight;
        if (baseDrawable != null) {
            baseDrawable.setBottomHeight(bottomHeight);
        }
    }

    @Override
    public float getMinWidth() {
        return baseDrawable != null ? baseDrawable.getMinWidth() : minWidth;
    }

    @Override
    public void setMinWidth(float minWidth) {
        this.minWidth = minWidth;
        if (baseDrawable != null) {
            baseDrawable.setMinWidth(minWidth);
        }
    }

    @Override
    public float getMinHeight() {
        return baseDrawable != null ? baseDrawable.getMinHeight() : minHeight;
    }

    @Override
    public void setMinHeight(float minHeight) {
        this.minHeight = minHeight;
        if (baseDrawable != null) {
            baseDrawable.setMinHeight(minHeight);
        }
    }

    @Override
    public void adopt(Disposable resource) {
        if (resource != null && !disposed) {
            bindings.add(resource);
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void dispose() {
        if (!disposed) {
            disposed = true;
            for (int i = 0; i < bindings.size(); i++) {
                Disposable d = bindings.get(i);
                if (d != null) {
                    d.dispose();
                }
            }
            bindings.clear();
        }
    }
}

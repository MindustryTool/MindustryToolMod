package solim.graphics;

import arc.Core;
import arc.fx.filters.CopyFilter;
import arc.fx.filters.GaussianBlurFilter;
import arc.fx.filters.GaussianBlurFilter.BlurType;
import arc.fx.util.PingPongBuffer;
import arc.graphics.Gl;
import arc.graphics.Pixmap.Format;
import arc.graphics.Texture;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.Texture.TextureWrap;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.ScissorStack;
import arc.graphics.g2d.TextureRegion;
import arc.graphics.gl.FrameBuffer;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.Scene;
import arc.struct.Seq;
import arc.util.Disposable;
import arc.util.Nullable;
import java.lang.reflect.Field;

@SuppressWarnings("unchecked")
public class BlurRenderer implements Disposable {

    private static final BlurRenderer INSTANCE = new BlurRenderer();
    private static final int DOWNSCALE = 4;

    private static @Nullable Seq<Rect> scissorSeq;

    static {
        try {
            Field f = ScissorStack.class.getDeclaredField("scissors");
            f.setAccessible(true);
            scissorSeq = (Seq<Rect>) f.get(null);
        } catch (Throwable ignored) {
            scissorSeq = null;
        }
    }

    private @Nullable Texture screenTexture;
    private @Nullable CopyFilter copyFilter;
    private @Nullable PingPongBuffer pingPongBuffer;
    private @Nullable GaussianBlurFilter blurFilter;
    private @Nullable FrameBuffer contentBuffer;
    private int bufferWidth = 0;
    private int bufferHeight = 0;
    private int downWidth = 0;
    private int downHeight = 0;
    private int contentWidth = 0;
    private int contentHeight = 0;

    private final Vec2 tmpVec = new Vec2();
    private final Rect stageRect = new Rect();
    private final Rect screenRect = new Rect();
    private final TextureRegion blurRegion = new TextureRegion();

    private @Nullable Rect currentScissor() {
        if (scissorSeq != null) {
            return scissorSeq.size > 0 ? scissorSeq.peek() : null;
        }
        try {
            return ScissorStack.peek();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static BlurRenderer get() {
        return INSTANCE;
    }

    public boolean isSupported() {
        return Core.graphics != null && Core.gl != null && Core.batch != null && Core.graphics.getWidth() > 0
                && Core.graphics.getHeight() > 0;
    }

    public boolean isEnabled() {
        return isSupported() && (Core.settings == null || Core.settings.getBool("solim.blur.enabled", true));
    }

    public void setEnabled(boolean enabled) {
        if (Core.settings != null) {
            Core.settings.put("solim.blur.enabled", enabled);
        }
    }

    private boolean ensureBuffers(int width, int height) {
        if (width <= 0 || height <= 0 || !isSupported()) {
            return false;
        }
        int downW = Math.max(1, width / DOWNSCALE);
        int downH = Math.max(1, height / DOWNSCALE);

        if (pingPongBuffer == null || blurFilter == null || copyFilter == null || screenTexture == null) {
            try {
                if (screenTexture != null) {
                    screenTexture.dispose();
                }
                screenTexture = new Texture(width, height);
                screenTexture.setFilter(TextureFilter.linear);
                screenTexture.setWrap(TextureWrap.clampToEdge);

                copyFilter = new CopyFilter();
                pingPongBuffer = new PingPongBuffer(Format.rgba8888, downW, downH, false, false);
                blurFilter = new GaussianBlurFilter(BlurType.gaussian5x5);
                blurFilter.resize(downW, downH);
                blurFilter.setPasses(1);
                blurFilter.setAmount(2.0f);

                bufferWidth = width;
                bufferHeight = height;
                downWidth = downW;
                downHeight = downH;
            } catch (Throwable t) {
                disposeBuffers();
                return false;
            }
        } else if (bufferWidth != width || bufferHeight != height) {
            try {
                bufferWidth = width;
                bufferHeight = height;
                downWidth = downW;
                downHeight = downH;

                screenTexture.dispose();
                screenTexture = new Texture(width, height);
                screenTexture.setFilter(TextureFilter.linear);
                screenTexture.setWrap(TextureWrap.clampToEdge);

                pingPongBuffer.resize(downW, downH);
                blurFilter.resize(downW, downH);
            } catch (Throwable t) {
                disposeBuffers();
                return false;
            }
        }
        return true;
    }

    public void drawBackdropBlur(@Nullable Element element, float drawX, float drawY, float drawWidth, float drawHeight,
            float radius, float parentAlpha) {
        if (!isEnabled() || radius <= 0.01f || element == null) {
            return;
        }
        Scene stage = element.getScene() != null ? element.getScene() : Core.scene;
        if (stage == null) {
            return;
        }
        int screenW = Core.graphics.getWidth();
        int screenH = Core.graphics.getHeight();
        if (screenW <= 1 || screenH <= 1 || !ensureBuffers(screenW, screenH)) {
            return;
        }
        PingPongBuffer ppb = pingPongBuffer;
        GaussianBlurFilter bf = blurFilter;
        CopyFilter cf = copyFilter;
        Texture st = screenTexture;
        if (ppb == null || bf == null || cf == null || st == null) {
            return;
        }

        element.localToStageCoordinates(tmpVec.set(0f, 0f));
        stageRect.set(tmpVec.x, tmpVec.y, element.getWidth(), element.getHeight());
        stage.calculateScissors(stageRect, screenRect);

        Rect currentScissor = currentScissor();
        if (currentScissor != null) {
            if (!screenRect.overlaps(currentScissor)) {
                return;
            }
            float minX = Math.max(screenRect.x, currentScissor.x);
            float minY = Math.max(screenRect.y, currentScissor.y);
            float maxX = Math.min(screenRect.x + screenRect.width, currentScissor.x + currentScissor.width);
            float maxY = Math.min(screenRect.y + screenRect.height, currentScissor.y + currentScissor.height);
            screenRect.set(minX, minY, Math.max(0f, maxX - minX), Math.max(0f, maxY - minY));
        }

        int sx = Math.max(0, (int) screenRect.x);
        int sy = Math.max(0, (int) screenRect.y);
        int sw = Math.min(screenW - sx, (int) screenRect.width);
        int sh = Math.min(screenH - sy, (int) screenRect.height);
        if (sw <= 1 || sh <= 1) {
            return;
        }

        Draw.flush();

        st.bind(0);
        Gl.copyTexSubImage2D(Gl.texture2d, 0, sx, sy, sx, sy, sw, sh);

        int dsx = sx / DOWNSCALE;
        int dsy = sy / DOWNSCALE;
        int dsw = Math.min(downWidth - dsx, Math.max(1, sw / DOWNSCALE));
        int dsh = Math.min(downHeight - dsy, Math.max(1, sh / DOWNSCALE));

        Gl.enable(Gl.scissorTest);
        Gl.scissor(dsx, dsy, dsw, dsh);

        cf.setInput(st).setOutput(ppb.getSrcBuffer()).render();

        int passes = radius > 30f ? 2 : 1;
        bf.setPasses(passes);
        bf.setAmount(2.0f);
        bf.render(ppb);

        if (currentScissor != null) {
            Gl.scissor((int) currentScissor.x, (int) currentScissor.y, (int) currentScissor.width,
                    (int) currentScissor.height);
        } else {
            Gl.disable(Gl.scissorTest);
        }

        Texture dstTex = ppb.getDstTexture();
        float u = (float) dsx / downWidth;
        float v = (float) (dsy + dsh) / downHeight;
        float u2 = (float) (dsx + dsw) / downWidth;
        float v2 = (float) dsy / downHeight;

        blurRegion.set(dstTex);
        blurRegion.set(u, v, u2, v2);

        Draw.color(1f, 1f, 1f, parentAlpha);
        Draw.rect(blurRegion, drawX + drawWidth / 2f, drawY + drawHeight / 2f, drawWidth, drawHeight);
        Draw.flush();
    }

    public void beginContentBlur(@Nullable Element element) {
        if (!isEnabled() || element == null) {
            return;
        }
        int cw = Math.max(1, (int) element.getWidth());
        int ch = Math.max(1, (int) element.getHeight());
        if (contentBuffer == null) {
            try {
                contentBuffer = new FrameBuffer(Format.rgba8888, cw, ch, false, false);
                contentWidth = cw;
                contentHeight = ch;
            } catch (Throwable t) {
                contentBuffer = null;
                return;
            }
        } else if (contentWidth < cw || contentHeight < ch) {
            try {
                contentWidth = Math.max(contentWidth, cw);
                contentHeight = Math.max(contentHeight, ch);
                contentBuffer.resize(contentWidth, contentHeight);
            } catch (Throwable t) {
                contentBuffer = null;
                return;
            }
        }
        Draw.flush();
        contentBuffer.begin();
        Gl.clearColor(0f, 0f, 0f, 0f);
        Gl.clear(Gl.colorBufferBit);
    }

    public void endContentBlur(@Nullable Element element, float radius) {
        if (contentBuffer == null || !contentBuffer.isBound() || element == null) {
            return;
        }
        Draw.flush();
        contentBuffer.end();

        int screenW = Core.graphics.getWidth();
        int screenH = Core.graphics.getHeight();
        if (screenW <= 1 || screenH <= 1 || !ensureBuffers(screenW, screenH)) {
            return;
        }
        PingPongBuffer ppb = pingPongBuffer;
        GaussianBlurFilter bf = blurFilter;
        CopyFilter cf = copyFilter;
        if (ppb == null || bf == null || cf == null) {
            return;
        }

        int ew = Math.max(1, (int) element.getWidth());
        int eh = Math.max(1, (int) element.getHeight());
        int dew = Math.min(downWidth, Math.max(1, ew / DOWNSCALE));
        int deh = Math.min(downHeight, Math.max(1, eh / DOWNSCALE));

        Gl.enable(Gl.scissorTest);
        Gl.scissor(0, 0, dew, deh);

        cf.setInput(contentBuffer.getTexture()).setOutput(ppb.getSrcBuffer()).render();

        int passes = radius > 30f ? 2 : 1;
        bf.setPasses(passes);
        bf.setAmount(2.0f);
        bf.render(ppb);

        Rect currentScissor = currentScissor();
        if (currentScissor != null) {
            Gl.scissor((int) currentScissor.x, (int) currentScissor.y, (int) currentScissor.width,
                    (int) currentScissor.height);
        } else {
            Gl.disable(Gl.scissorTest);
        }

        Texture dstTex = ppb.getDstTexture();
        float u = 0f;
        float v = (float) deh / downHeight;
        float u2 = (float) dew / downWidth;
        float v2 = 0f;

        blurRegion.set(dstTex);
        blurRegion.set(u, v, u2, v2);

        Draw.color(1f, 1f, 1f, 1f);
        Draw.rect(blurRegion, element.x + element.getWidth() / 2f, element.y + element.getHeight() / 2f,
                element.getWidth(), element.getHeight());
        Draw.flush();
    }

    @Override
    public void dispose() {
        disposeBuffers();
    }

    private void disposeBuffers() {
        if (screenTexture != null) {
            screenTexture.dispose();
            screenTexture = null;
        }
        if (copyFilter != null) {
            copyFilter.dispose();
            copyFilter = null;
        }
        if (pingPongBuffer != null) {
            pingPongBuffer.dispose();
            pingPongBuffer = null;
        }
        if (blurFilter != null) {
            blurFilter.dispose();
            blurFilter = null;
        }
        if (contentBuffer != null) {
            contentBuffer.dispose();
            contentBuffer = null;
        }
        bufferWidth = 0;
        bufferHeight = 0;
        downWidth = 0;
        downHeight = 0;
        contentWidth = 0;
        contentHeight = 0;
    }
}

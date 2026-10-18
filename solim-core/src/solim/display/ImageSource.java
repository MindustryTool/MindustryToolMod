package solim.display;

import arc.files.Fi;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Nullable;
import java.util.Objects;

public interface ImageSource {

    @Nullable
    Drawable drawable();

    @Nullable
    String url();

    @Nullable
    Fi file();

    default boolean isAsync() {
        return drawable() == null;
    }

    static ImageSource of(@Nullable Drawable drawable) {
        return new DrawableImageSource(drawable);
    }

    static ImageSource of(@Nullable TextureRegion region) {
        return new DrawableImageSource(region != null ? new TextureRegionDrawable(region) : null);
    }

    static ImageSource of(@Nullable String url) {
        return new UrlImageSource(url);
    }

    static ImageSource of(@Nullable Fi file) {
        return new FiImageSource(file);
    }

    final class DrawableImageSource implements ImageSource {

        private final @Nullable Drawable drawable;

        private DrawableImageSource(@Nullable Drawable drawable) {
            this.drawable = drawable;
        }

        @Override
        public @Nullable Drawable drawable() {
            return drawable;
        }

        @Override
        public @Nullable String url() {
            return null;
        }

        @Override
        public @Nullable Fi file() {
            return null;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof DrawableImageSource
                    && Objects.equals(drawable, ((DrawableImageSource) other).drawable);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(drawable);
        }
    }

    final class UrlImageSource implements ImageSource {

        private final @Nullable String url;

        private UrlImageSource(@Nullable String url) {
            this.url = url;
        }

        @Override
        public @Nullable Drawable drawable() {
            return null;
        }

        @Override
        public @Nullable String url() {
            return url;
        }

        @Override
        public @Nullable Fi file() {
            return null;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof UrlImageSource
                    && Objects.equals(url, ((UrlImageSource) other).url);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(url);
        }
    }

    final class FiImageSource implements ImageSource {

        private final @Nullable Fi file;

        private FiImageSource(@Nullable Fi file) {
            this.file = file;
        }

        @Override
        public @Nullable Drawable drawable() {
            return null;
        }

        @Override
        public @Nullable String url() {
            return null;
        }

        @Override
        public @Nullable Fi file() {
            return file;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof FiImageSource
                    && Objects.equals(file, ((FiImageSource) other).file);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(file);
        }
    }
}

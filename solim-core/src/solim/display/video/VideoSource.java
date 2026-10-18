package solim.display.video;

import arc.files.Fi;
import arc.util.Nullable;
import java.util.Objects;

public interface VideoSource {

    @Nullable
    String url();

    @Nullable
    Fi file();

    default boolean isLocal() {
        return file() != null;
    }

    static VideoSource of(@Nullable String url) {
        return new UrlVideoSource(url);
    }

    static VideoSource of(@Nullable Fi file) {
        return new FiVideoSource(file);
    }

    final class UrlVideoSource implements VideoSource {

        private final @Nullable String url;

        private UrlVideoSource(@Nullable String url) {
            this.url = url;
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
            return other instanceof UrlVideoSource
                    && Objects.equals(url, ((UrlVideoSource) other).url);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(url);
        }
    }

    final class FiVideoSource implements VideoSource {

        private final @Nullable Fi file;

        private FiVideoSource(@Nullable Fi file) {
            this.file = file;
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
            return other instanceof FiVideoSource
                    && Objects.equals(file, ((FiVideoSource) other).file);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(file);
        }
    }
}

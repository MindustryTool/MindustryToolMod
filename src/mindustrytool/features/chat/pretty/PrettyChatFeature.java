package mindustrytool.features.chat.pretty;

import java.util.Optional;
import java.util.function.Function;

import arc.scene.ui.Dialog;
import arc.struct.Seq;
import arc.util.Log;
import lombok.AllArgsConstructor;
import lombok.Getter;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

public class PrettyChatFeature implements Feature {

    @Getter
    private static final Seq<Prettier> prettiers = new Seq<>();

    private static boolean enabled = false;

    static {
        prettiers.add(new Prettier("default", "Default", s -> s));
        prettiers.add(new Prettier("uwu", "UwUifier", PrettyChatFeature::uwuify));
        prettiers.add(new Prettier("caps", "CAPS LOCK", String::toUpperCase));
        prettiers.add(new Prettier("lowercase", "lowercase", String::toLowerCase));
        prettiers.add(new Prettier("reverse", "esreveR", s -> new StringBuilder(s).reverse().toString()));
    }

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.prettychat.name")
                .description("@feature.prettychat.description")
                .icon(Icon.chat)
                .build();
    }

    @Override
    public void init() {

    }

    @Override
    public void onEnable() {
        enabled = true;
    }

    @Override
    public void onDisable() {
        enabled = false;
    }

    @Override
    public Optional<Dialog> setting() {
        return Optional.of(new PrettyChatSettingsDialog());
    }

    public static String transform(String message) {
        Log.info("transform: " + message);
        if (!enabled) {
            return message;
        }

        String result = message;
        Seq<String> enabledIds = PrettyChatConfig.getEnabledIds();

        Log.info(enabledIds);
        for (String id : enabledIds) {
            Log.info(id);
            Prettier p = prettiers.find(x -> x.id.equals(id));
            if (p != null) {
                result = p.transform.apply(result);
                Log.info(result);
            }
        }
        return result;
    }

    private static String uwuify(String text) {
        return text
                .replace("r", "w").replace("R", "W")
                .replace("l", "w").replace("L", "W")
                .replace("ove", "uv")
                + " uwu";
    }

    @Getter
    @AllArgsConstructor
    public static class Prettier {
        private String id;
        private String name;
        private Function<String, String> transform;
    }
}

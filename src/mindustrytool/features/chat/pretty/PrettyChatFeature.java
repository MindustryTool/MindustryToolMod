package mindustrytool.features.chat.pretty;

import java.util.Optional;

import arc.Core;
import arc.Events;
import arc.scene.ui.Dialog;
import arc.scene.ui.TextField;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Reflect;
import lombok.AllArgsConstructor;
import lombok.Getter;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Icon;
import mindustry.input.Binding;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

public class PrettyChatFeature implements Feature {

    @Getter
    private static final Seq<Prettier> prettiers = new Seq<>();

    private static boolean enabled = false;

    static {
        prettiers.add(new Prettier(
                "default",
                "Default",
                "<message>"));

        prettiers.add(new Prettier(
                "uwu",
                "UwUifier",
                "<message>"
                        + ".replace(/r/g,'w').replace(/R/g,'W')"
                        + ".replace(/l/g,'w').replace(/L/g,'W')"
                        + ".replace(/ove/g,'uv')"
                        + " + ' uwu'"));

        prettiers.add(new Prettier(
                "caps",
                "CAPS LOCK",
                "<message>.toUpperCase()"));

        prettiers.add(new Prettier(
                "lowercase",
                "lowercase",
                "<message>.toLowerCase()"));

        prettiers.add(new Prettier(
                "reverse",
                "esreveR",
                "<message>.split('').reverse().join('')"));

        prettiers.add(new Prettier(
                "rainbow",
                "rainbow",
                "(function(){"
                        + "var c=['red','orange','yellow','green','cyan','blue','purple'];"
                        + "var o='';"
                        + "var ci=0;"
                        + "var words=" + "<message>" + ".split(' ');"
                        + "for(var j=0;j<words.length;j++){"
                        + "if(j>0)o+=' ';"
                        + "var word=words[j];"
                        + "for(var i=0;i<word.length;i++){"
                        + "o+='['+c[ci%c.length]+']'+word.charAt(i);"
                        + "}"
                        + "ci++;"
                        + "}"
                        + "return o+'[]';"
                        + "})()"));
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
        Events.run(Trigger.update, () -> {
            if (Core.input.keyTap(Binding.chat) && Vars.ui.chatfrag.shown()) {
                Core.app.post(() -> {
                    try {
                        TextField chatfield = Reflect.get(Vars.ui.chatfrag, "chatfield");

                        if (chatfield == null) {
                            return;
                        }

                        String formatted = transform(chatfield.getText());
                        chatfield.setText(formatted.substring(0, Math.min(Vars.maxTextLength, formatted.length())));
                    } catch (Exception e) {
                        Log.err(e);
                    }
                });
            }
        });
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
        if (!enabled) {
            return message;
        }

        if (message.isEmpty()) {
            return message;
        }

        var cmd = "";

        if (message.startsWith("/")) {
            var spaceIndex = message.indexOf(' ');
            var subIndex = spaceIndex == -1 ? message.length() : spaceIndex;
            cmd = message.substring(0, subIndex);

            if (!cmd.equals("/t") && !cmd.equals("/a")) {
                return message;
            }
            message = message.substring(subIndex);
        }

        String result = message;
        Seq<String> enabledIds = PrettyChatConfig.getEnabledIds();

        for (String id : enabledIds) {
            Prettier p = prettiers.find(x -> x.id.equals(id));
            if (p != null) {
                result = transform(result, p);
            }
        }

        return cmd + result;
    }

    public static String transform(String message, Prettier prettier) {
        var result = Vars.mods.getScripts().runConsole(prettier.getScript().replace("<message>", '"' + message + '"'));

        if (result == null) {
            return "Script: " + prettier.getScript() + "\nreturn null";
        }

        return result;
    }

    @Getter
    @AllArgsConstructor
    public static class Prettier {
        private String id;
        private String name;
        private String defaultScript;

        public String getScript() {
            return PrettyChatConfig.getScript(id, defaultScript);
        }
    }
}

package mindustrytool.features.prettychat;

import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.reactive.Signal;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages configuration and reactive state for Pretty Chat.
 */
public class PrettyChatConfig {

    public final ConfigGroup configGroup;
    public final ConfigValue<String> enabledPrettiersConfig;
    public final Signal<List<String>> enabledIdsSignal;

    public PrettyChatConfig(ConfigGroup configGroup) {
        this.configGroup = configGroup;
        this.enabledPrettiersConfig = configGroup.stringValue("enabled-prettiers", "rainbow");

        List<String> initial = parseList(enabledPrettiersConfig.signal().peek());
        this.enabledIdsSignal = Signal.of(initial);

        this.enabledPrettiersConfig.signal().subscribe(val -> {
            List<String> parsed = parseList(val);
            enabledIdsSignal.set(parsed);
        });
    }

    public List<String> getEnabledIds() {
        return new ArrayList<>(enabledIdsSignal.peek());
    }

    public boolean isEnabled(String id) {
        return enabledIdsSignal.peek().contains(id);
    }

    public void setEnabledIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            enabledPrettiersConfig.set("");
            enabledIdsSignal.set(new ArrayList<>());
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(ids.get(i));
        }
        enabledPrettiersConfig.set(sb.toString());
        enabledIdsSignal.set(new ArrayList<>(ids));
    }

    public void toggle(String id) {
        List<String> current = getEnabledIds();
        if (current.contains(id)) {
            current.remove(id);
        } else {
            current.add(id);
        }
        setEnabledIds(current);
    }

    public void move(String id, int offset) {
        List<String> current = getEnabledIds();
        int index = current.indexOf(id);
        if (index == -1) {
            return;
        }
        int target = index + offset;
        if (target < 0 || target >= current.size()) {
            return;
        }
        current.remove(index);
        current.add(target, id);
        setEnabledIds(current);
    }

    private static List<String> parseList(String raw) {
        List<String> list = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return list;
        }
        String[] parts = raw.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && !list.contains(trimmed)) {
                list.add(trimmed);
            }
        }
        return list;
    }
}

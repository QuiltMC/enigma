package cuchaz.enigma.gui.config;

import cuchaz.enigma.config.ConfigContainer;
import cuchaz.enigma.config.ConfigSection;
import cuchaz.enigma.source.quiltflower.QuiltflowerPreferences;

import java.util.HashSet;
import java.util.Map;

public class DecompilerConfig {
    private DecompilerConfig() {
    }

    private static final ConfigContainer cfg = ConfigContainer.getOrCreate("enigma/decompilers");

    public static void save() {
        cfg.save();
    }

    private static ConfigSection getQuiltflowerSection() {
        return cfg.data().section("Quiltflower");
    }

    public static void updateQuiltflowerValues(Map<String, Object> options) {
        ConfigSection section = getQuiltflowerSection();
        new HashSet<>(section.values().keySet()).forEach(section::remove);

        for (Map.Entry<String, Object> entry : options.entrySet()) {
            if (entry.getValue() instanceof String s) {
                section.setString(entry.getKey(), s);
            }
        }
    }

    public static void bootstrap() {
    }

    static {
        QuiltflowerPreferences.OPTIONS.putAll(getQuiltflowerSection().values());
    }
}

package cuchaz.enigma.gui.config;

import cuchaz.enigma.config.ConfigContainer;
import cuchaz.enigma.config.ConfigSection;
import cuchaz.enigma.gui.config.keybind.KeyBind;

public final class KeyBindsConfig {
    private KeyBindsConfig() {
    }

    private static final ConfigContainer cfg = ConfigContainer.getOrCreate("enigma/enigmakeybinds");

    private static ConfigSection runningCfg;

    static {
        KeyBindsConfig.snapshotConfig();
    }

    // Save the current configuration state so consistent keybinds can be provided
    // for keybinds that are not configurable during runtime.
    public static void snapshotConfig() {
        runningCfg = cfg.data().copy();
    }

    public static void save() {
        cfg.save();
    }

    private static ConfigSection getSection(KeyBind keyBind, boolean running) {
        if (running) {
            return keyBind.category().isEmpty() ? runningCfg : runningCfg.section(keyBind.category());
        } else {
            return keyBind.category().isEmpty() ? cfg.data() : cfg.data().section(keyBind.category());
        }
    }

    public static String[] getKeyBindCodes(KeyBind keyBind) {
        return getSection(keyBind, true).setIfAbsentArray(keyBind.name(), keyBind.serializeCombinations());
    }

    public static void setKeyBind(KeyBind keyBind) {
        getSection(keyBind, false).setArray(keyBind.name(), keyBind.serializeCombinations());
    }
}

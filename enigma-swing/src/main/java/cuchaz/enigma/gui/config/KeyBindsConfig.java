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
            return keyBind.getCategory().isEmpty() ? runningCfg:runningCfg.section(keyBind.getCategory());
        } else {
            return keyBind.getCategory().isEmpty() ? cfg.data() : cfg.data().section(keyBind.getCategory());
        }
    }

    public static int[] getKeyBindCodes(KeyBind keyBind) {
        return getSection(keyBind, true).setIfAbsentIntArray(keyBind.getName(), keyBind.getKeyCodesArray());
    }

    public static void setKeyBind(KeyBind keyBind) {
        getSection(keyBind, false).setIntArray(keyBind.getName(), keyBind.getKeyCodesArray());
    }
}

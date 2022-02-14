package cuchaz.enigma.gui.config.keybind;

import cuchaz.enigma.gui.config.KeyBindsConfig;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class KeyBinds {
    private static final String QUICK_FIND_DIALOG_CATEGORY = "quick_find_dialog";
    private static final String SEARCH_DIALOG_CATEGORY = "search_dialog";
    private static final String EDITOR_CATEGORY = "editor";
    private static final String MENU_CATEGORY = "menu";

    public static final KeyBind EXIT = new KeyBind("close", KeyEvent.VK_ESCAPE);
    public static final KeyBind DIALOG_SAVE = new KeyBind("dialog_save", KeyEvent.VK_ENTER);

    public static final KeyBind QUICK_FIND_DIALOG_NEXT = new KeyBind("next", KeyEvent.VK_ENTER, QUICK_FIND_DIALOG_CATEGORY);
    public static final KeyBind SEARCH_DIALOG_NEXT = new KeyBind("next", KeyEvent.VK_DOWN, SEARCH_DIALOG_CATEGORY);
    public static final KeyBind SEARCH_DIALOG_PREVIOUS = new KeyBind("previous", KeyEvent.VK_UP, SEARCH_DIALOG_CATEGORY);

    public static final KeyBind EDITOR_RENAME = new KeyBind("rename", KeyEvent.VK_R, EDITOR_CATEGORY);
    public static final KeyBind EDITOR_PASTE = new KeyBind("paste", KeyEvent.VK_V, EDITOR_CATEGORY);
    public static final KeyBind EDITOR_EDIT_JAVADOC = new KeyBind("edit_javadoc", KeyEvent.VK_D, EDITOR_CATEGORY);
    public static final KeyBind EDITOR_SHOW_INHERITANCE = new KeyBind("show_inheritance", KeyEvent.VK_I, EDITOR_CATEGORY);
    public static final KeyBind EDITOR_SHOW_IMPLEMENTATIONS = new KeyBind("show_implementations", KeyEvent.VK_M, EDITOR_CATEGORY);
    public static final KeyBind EDITOR_SHOW_CALLS = new KeyBind("show_calls", KeyEvent.VK_C, EDITOR_CATEGORY);
    public static final KeyBind EDITOR_OPEN_ENTRY = new KeyBind("open_entry", KeyEvent.VK_N, EDITOR_CATEGORY);
    public static final KeyBind EDITOR_OPEN_PREVIOUS = new KeyBind("open_previous", KeyEvent.VK_P, EDITOR_CATEGORY);
    public static final KeyBind EDITOR_OPEN_NEXT = new KeyBind("open_next", KeyEvent.VK_E, EDITOR_CATEGORY);
    public static final KeyBind EDITOR_TOGGLE_MAPPING = new KeyBind("toggle_mapping", KeyEvent.VK_O, EDITOR_CATEGORY);
    public static final KeyBind EDITOR_ZOOM_IN = new KeyBind("zoom_in", EDITOR_CATEGORY, KeyEvent.VK_PLUS, KeyEvent.VK_ADD, KeyEvent.VK_EQUALS);
    public static final KeyBind EDITOR_ZOOM_OUT = new KeyBind("zoom_out", EDITOR_CATEGORY, KeyEvent.VK_MINUS, KeyEvent.VK_SUBTRACT);
    public static final KeyBind EDITOR_CLOSE_TAB = new KeyBind("close_tab", KeyEvent.VK_4, EDITOR_CATEGORY);
    public static final KeyBind EDITOR_RELOAD_CLASS = new KeyBind("reload_class", KeyEvent.VK_F5, EDITOR_CATEGORY);
    public static final KeyBind EDITOR_QUICK_FIND = new KeyBind("quick_find", KeyEvent.VK_F, EDITOR_CATEGORY);

    public static final KeyBind SAVE_MAPPINGS = new KeyBind("save", KeyEvent.VK_S, MENU_CATEGORY);
    public static final KeyBind SEARCH_CLASS = new KeyBind("search_class", KeyEvent.VK_SPACE, MENU_CATEGORY);

    public static final List<KeyBind> CONFIGURABLE_KEY_BINDS = List.of(EDITOR_RENAME, EDITOR_PASTE, EDITOR_EDIT_JAVADOC,
            EDITOR_SHOW_INHERITANCE, EDITOR_SHOW_IMPLEMENTATIONS, EDITOR_SHOW_CALLS, EDITOR_OPEN_ENTRY,
            EDITOR_OPEN_PREVIOUS, EDITOR_OPEN_NEXT, EDITOR_TOGGLE_MAPPING, EDITOR_ZOOM_IN, EDITOR_ZOOM_OUT,
            EDITOR_CLOSE_TAB, EDITOR_RELOAD_CLASS, SAVE_MAPPINGS, SEARCH_CLASS);

    private KeyBinds() {
    }

    public static boolean isConfigurable(KeyBind keyBind) {
        return CONFIGURABLE_KEY_BINDS.contains(keyBind);
    }

    public static Map<String, List<KeyBind>> getConfigurableKeyBindsByCategory() {
        return CONFIGURABLE_KEY_BINDS.stream()
                .collect(Collectors.groupingBy(KeyBind::getCategory));
    }

    public static void loadConfig() {
        for (KeyBind keyBind : CONFIGURABLE_KEY_BINDS) {
            keyBind.setKeyCodes(KeyBindsConfig.getKeyBindCodes(keyBind));
        }
    }
}

package cuchaz.enigma.gui.config.keybind;

import cuchaz.enigma.utils.I18n;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class KeyBind {
    private final String name;
    private final String category;
    private final List<Integer> keyCodes;
    private final int modifiers;

    private KeyBind(String name, String category, List<Integer> keyCodes, int modifiers) {
        this.name = name;
        this.category = category;
        this.keyCodes = new ArrayList<>(keyCodes);
        this.modifiers = modifiers;
    }

    public boolean matches(KeyEvent e) {
        return this.keyCodes.contains(e.getKeyCode());
    }

    public KeyStroke toKeyStroke() {
        return this.toKeyStroke(0);
    }

    public KeyStroke toKeyStroke(int modifiers) {
        modifiers = this.modifiers | modifiers;
        return hasKeyCodes() ? KeyStroke.getKeyStroke(getFirst(), modifiers) : null;
    }

    public String getName() {
        return this.name;
    }

    public String getCategory() {
        return this.category;
    }

    public List<Integer> getKeyCodes() {
        return this.keyCodes;
    }

    public int getModifiers() {
        return this.modifiers;
    }

    public int[] getKeyCodesArray() {
        return this.keyCodes.stream().mapToInt(Integer::intValue).toArray();
    }

    private String getTranslationKey() {
        return "keybind." + (this.category.isEmpty() ? "" : this.category + ".") + this.name;
    }

    public String getTranslatedName() {
        return I18n.translate(this.getTranslationKey());
    }

    protected void setKeyCodes(int... keyCodes) {
        this.keyCodes.clear();
        for (int keyCode : keyCodes) {
            if (!this.keyCodes.contains(keyCode)) {
                this.keyCodes.add(keyCode);
            }
        }
    }

    public void addKeyCode(int keyCode) {
        if (!this.keyCodes.contains(keyCode)) {
            this.keyCodes.add(keyCode);
        }
    }

    public void removeKeyCode(int keyCode) {
        this.keyCodes.remove((Object) keyCode);
    }

    public void clearKeyCodes() {
        this.keyCodes.clear();
    }

    public int getFirst() {
        return this.keyCodes.get(0);
    }

    public int getLast() {
        return this.keyCodes.get(this.keyCodes.size() - 1);
    }

    public boolean hasKeyCodes() {
        return !this.keyCodes.isEmpty();
    }

    public KeyBind copy() {
        return new KeyBind(this.name, this.category, this.keyCodes, this.modifiers);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static Builder builder(String name, String category) {
        return new Builder(name, category);
    }

    public static class Builder {
        private final String name;
        private final String category;
        private final List<Integer> keyCodes = new ArrayList<>();
        private int modifiers = 0;

        public Builder(String name) {
            this.name = name;
            this.category = "";
        }

        public Builder(String name, String category) {
            this.name = name;
            this.category = category;
        }

        public Builder key(int keyCode) {
            keyCodes.add(keyCode);
            return this;
        }

        public Builder keys(int... keyCodes) {
            for (int keyCode : keyCodes) {
                this.keyCodes.add(keyCode);
            }
            return this;
        }

        public Builder mod(int modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        public KeyBind build() {
            return new KeyBind(name, category, keyCodes, modifiers);
        }
    }
}

package cuchaz.enigma.gui.config.keybind;

import cuchaz.enigma.utils.I18n;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class KeyBind {
    private final String name;
    private final String category;
    private final SortedSet<Integer> keyCodes;

    public KeyBind(String name, int keyCode) {
        this.name = name;
        this.category = "";
        this.keyCodes = new TreeSet<>();
        this.keyCodes.add(keyCode);
    }

    public KeyBind(String name, int keyCode, String category) {
        this.name = name;
        this.category = category;
        this.keyCodes = new TreeSet<>();
        this.keyCodes.add(keyCode);
    }

    public KeyBind(String name, int... keyCodes) {
        this.name = name;
        this.category = "";
        this.keyCodes = new TreeSet<>();
        for (int keyCode : keyCodes) {
            this.keyCodes.add(keyCode);
        }
    }

    public KeyBind(String name, String category, int... keyCodes) {
        this.name = name;
        this.category = category;
        this.keyCodes = new TreeSet<>();
        for (int keyCode : keyCodes) {
            this.keyCodes.add(keyCode);
        }
    }

    public KeyBind(String name, String category, Set<Integer> keyCodes) {
        this.name = name;
        this.category = category;
        this.keyCodes = new TreeSet<>(keyCodes);
    }

    public boolean matches(KeyEvent e) {
        return this.keyCodes.contains(e.getKeyCode());
    }

    public KeyStroke toKeyStroke(int modifiers) {
        return hasKeyCodes() ? KeyStroke.getKeyStroke(getFirst(), modifiers) : null;
    }

    public String getName() {
        return this.name;
    }

    public String getCategory() {
        return this.category;
    }

    public Set<Integer> getKeyCodes() {
        return this.keyCodes;
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
            this.keyCodes.add(keyCode);
        }
    }

    public void addKeyCode(int keyCode) {
        this.keyCodes.add(keyCode);
    }

    public void removeKeyCode(int keyCode) {
        this.keyCodes.remove(keyCode);
    }

    public void clearKeyCodes() {
        this.keyCodes.clear();
    }

    public int getFirst() {
        return this.keyCodes.first();
    }

    public int getLast() {
        return this.keyCodes.last();
    }

    public boolean hasKeyCodes() {
        return !this.keyCodes.isEmpty();
    }

    public KeyBind copy() {
        return new KeyBind(this.name, this.category, this.keyCodes);
    }

    public KeyBind withKeyCodes(int... keyCodes) {
        return new KeyBind(this.name, this.category, keyCodes);
    }
}

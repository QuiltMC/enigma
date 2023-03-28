package cuchaz.enigma.gui.config.keybind;

import cuchaz.enigma.utils.I18n;
import org.tinylog.Logger;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record KeyBind(String name, String category, List<Combination> combinations) {
	public record Combination(int keyCode, int keyModifiers) {
		public static final Combination EMPTY = new Combination(-1, 0);

		public boolean matches(KeyEvent e) {
			return e.getKeyCode() == this.keyCode && e.getModifiersEx() == this.keyModifiers;
		}

		public KeyStroke toKeyStroke(int modifiers) {
			modifiers = this.keyModifiers | modifiers;
			return KeyStroke.getKeyStroke(this.keyCode, modifiers);
		}

		public String serialize() {
			return this.keyCode + ";" + Integer.toString(this.keyModifiers, 16);
		}

		public static Combination deserialize(String str) {
			String[] parts = str.split(";", 2);
			return new Combination(Integer.parseInt(parts[0]), Integer.parseInt(parts[1], 16));
		}

		@Override
		public String toString() {
			return "Combination[keyCode=" + this.keyCode + ", keyModifiers=0x" + Integer.toString(this.keyModifiers, 16).toUpperCase(Locale.ROOT) + "]";
		}
	}

	public void setFrom(KeyBind other) {
		this.combinations.clear();
		this.combinations.addAll(other.combinations);
	}

	public boolean matches(KeyEvent e) {
		return this.combinations.stream().anyMatch(c -> c.matches(e));
	}

	public KeyStroke toKeyStroke(int modifiers) {
		return this.isEmpty() ? null : this.combinations.get(0).toKeyStroke(modifiers);
	}

	public KeyStroke toKeyStroke() {
		return this.toKeyStroke(0);
	}

	public int getKeyCode() {
		return this.isEmpty() ? -1 : this.combinations.get(0).keyCode;
	}

	public boolean isEmpty() {
		return this.combinations.isEmpty();
	}

	public String[] serializeCombinations() {
		return this.combinations.stream().map(Combination::serialize).toArray(String[]::new);
	}

	public void deserializeCombinations(String[] serialized) {
		this.combinations.clear();
		for (String serializedCombination : serialized) {
			if (!serializedCombination.isEmpty()) {
				this.combinations.add(Combination.deserialize(serializedCombination));
			} else {
				Logger.warn("empty combination deserialized for keybind " + (this.category.isEmpty() ? "" : this.category + ".") + this.name);
			}
		}
	}

	private String getTranslationKey() {
		return "keybind." + (this.category.isEmpty() ? "" : this.category + ".") + this.name;
	}

	public String getTranslatedName() {
		return I18n.translate(this.getTranslationKey());
	}

	public KeyBind copy() {
		return new KeyBind(this.name, this.category, new ArrayList<>(this.combinations));
	}

	public KeyBind toImmutable() {
		return new KeyBind(this.name, this.category, List.copyOf(this.combinations));
	}

	public boolean isSameKeyBind(KeyBind other) {
		return this.name.equals(other.name) && this.category.equals(other.category);
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
		private final List<Combination> combinations = new ArrayList<>();
		private int modifiers = 0;

		private Builder(String name) {
			this.name = name;
			this.category = "";
		}

		private Builder(String name, String category) {
			this.name = name;
			this.category = category;
		}

		public KeyBind build() {
			return new KeyBind(this.name, this.category, this.combinations);
		}

		public Builder key(int keyCode, int keyModifiers) {
			this.combinations.add(new Combination(keyCode, keyModifiers | this.modifiers));
			return this;
		}

		public Builder key(int keyCode) {
			return this.key(keyCode, 0);
		}

		public Builder keys(int... keyCodes) {
			for (int keyCode : keyCodes) {
				this.key(keyCode);
			}

			return this;
		}

		public Builder mod(int modifiers) {
			this.modifiers |= modifiers;
			return this;
		}
	}
}

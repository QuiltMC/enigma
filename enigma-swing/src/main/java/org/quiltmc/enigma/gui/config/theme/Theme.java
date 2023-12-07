package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.SerializedName;
import org.quiltmc.config.api.values.ComplexConfigValue;
import org.quiltmc.config.api.values.ConfigSerializableObject;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueMap;

import java.awt.Color;
import java.awt.Font;

public class Theme extends ReflectiveConfig.Section {
	public final transient LookAndFeel lookAndFeel;
	public Theme(LookAndFeel lookAndFeel) {
		this.lookAndFeel = lookAndFeel;
	}

	@SerializedName("colors")
	public final Colors colors = new Colors();
	@SerializedName("fonts")
	public final Fonts fonts = new Fonts();

	public static class Fonts extends ReflectiveConfig.Section {
		@SerializedName("default")
		public final TrackedValue<SerializableFont> defaultFont = this.value(new SerializableFont(Font.decode(Font.DIALOG).deriveFont(Font.BOLD)));
		@SerializedName("small")
		public final TrackedValue<SerializableFont> small = this.value(new SerializableFont(Font.decode(Font.DIALOG)));
		@SerializedName("editor")
		public final TrackedValue<SerializableFont> editor = this.value(new SerializableFont(Font.decode(Font.MONOSPACED)));

		public static class SerializableFont extends Font implements ConfigSerializableObject<ValueMap<String>> {
			public SerializableFont(Font font) {
				this(font.getName(), font.getStyle(), font.getSize());
			}

			public SerializableFont(String name, int style, int size) {
				super(name, style, size);
			}

			@Override
			public ConfigSerializableObject<ValueMap<String>> convertFrom(ValueMap<String> representation) {
				return new SerializableFont(
					representation.get("name"),
					Integer.parseInt(representation.get("style")),
					Integer.parseInt(representation.get("size"))
				);
			}

			@Override
			public ValueMap<String> getRepresentation() {
				return ValueMap.builder("")
					.put("name", this.name)
					.put("style", String.valueOf(this.style))
					.put("size", String.valueOf(this.size))
					.build();
			}

			@Override
			public ComplexConfigValue copy() {
				return new SerializableFont(this.name, this.style, this.size);
			}
		}
	}

	public static class Colors extends ReflectiveConfig.Section {
		@SerializedName("line_numbers_foreground")
		public final TrackedValue<SerializableColor> lineNumbersForeground = this.value(new SerializableColor(0xFF333300));
		@SerializedName("line_numbers_background")
		public final TrackedValue<SerializableColor> lineNumbersBackground = this.value(new SerializableColor(0xFFEEEEFF));
		@SerializedName("line_numbers_selected")
		public final TrackedValue<SerializableColor> lineNumbersSelected = this.value(new SerializableColor(0xFFCCCCEE));
		@SerializedName("obfuscated")
		public final TrackedValue<SerializableColor> obfuscated = this.value(new SerializableColor(0xFFFFDCDC));
		@SerializedName("obfuscated_outline")
		public final TrackedValue<SerializableColor> obfuscatedOutline = this.value(new SerializableColor(0xFFA05050));

		@SerializedName("proposed")
		public final TrackedValue<SerializableColor> proposed = this.value(new SerializableColor(0xFF000000));
		@SerializedName("proposed_outline")
		public final TrackedValue<SerializableColor> proposedOutline = this.value(new SerializableColor(0xBF000000));

		@SerializedName("deobfuscated")
		public final TrackedValue<SerializableColor> deobfuscated = this.value(new SerializableColor(0xFFDCFFDC));
		@SerializedName("deobfuscated_outline")
		public final TrackedValue<SerializableColor> deobfuscatedOutline = this.value(new SerializableColor(0xFF50A050));

		@SerializedName("editor_background")
		public final TrackedValue<SerializableColor> editorBackground = this.value(new SerializableColor(0xFF50A050));
		@SerializedName("highlight")
		public final TrackedValue<SerializableColor> highlight = this.value(new SerializableColor(0xFF50A050));
		@SerializedName("caret")
		public final TrackedValue<SerializableColor> caret = this.value(new SerializableColor(0xFF50A050));
		@SerializedName("selection_highlight")
		public final TrackedValue<SerializableColor> selectionHighlight = this.value(new SerializableColor(0xFF50A050));
		@SerializedName("string")
		public final TrackedValue<SerializableColor> string = this.value(new SerializableColor(0xFFCC6600));
		@SerializedName("number")
		public final TrackedValue<SerializableColor> number = this.value(new SerializableColor(0xFF999933));
		@SerializedName("operator")
		public final TrackedValue<SerializableColor> operator = this.value(new SerializableColor(0xFF000000));
		@SerializedName("delimiter")
		public final TrackedValue<SerializableColor> delimiter = this.value(new SerializableColor(0xFF000000));
		@SerializedName("type")
		public final TrackedValue<SerializableColor> type = this.value(new SerializableColor(0xFF000000));
		@SerializedName("identifier")
		public final TrackedValue<SerializableColor> identifier = this.value(new SerializableColor(0xFF000000));
		@SerializedName("comment")
		public final TrackedValue<SerializableColor> comment = this.value(new SerializableColor(0xFF339933));
		@SerializedName("text")
		public final TrackedValue<SerializableColor> text = this.value(new SerializableColor(0xFF000000));

		@SerializedName("debug_token")
		public final TrackedValue<SerializableColor> debugToken = this.value(new SerializableColor(0xFFD9BEF9));
		@SerializedName("debug_token_outline")
		public final TrackedValue<SerializableColor> debugTokenOutline = this.value(new SerializableColor(0xFFBD93F9));

		@SerializedName("dock_highlight")
		public final TrackedValue<SerializableColor> dockHighlight = this.value(new SerializableColor(0xFF0000FF));

		private static class SerializableColor extends Color implements ConfigSerializableObject<Integer> {
			private final int rgba;

			SerializableColor(int rgba) {
				super(rgba, true);
				this.rgba = rgba;
			}

			@Override
			public ConfigSerializableObject<Integer> convertFrom(Integer representation) {
				return new SerializableColor(representation);
			}

			@Override
			public Integer getRepresentation() {
				return this.rgba;
			}

			@Override
			public ComplexConfigValue copy() {
				return new SerializableColor(this.rgba);
			}
		}

		public void configure(boolean dark) {
			if (dark) {
				setIfAbsent(this.lineNumbersForeground, new SerializableColor(0xFFA4A4A3));
				setIfAbsent(this.lineNumbersBackground, new SerializableColor(0xFF313335));
				setIfAbsent(this.lineNumbersSelected, new SerializableColor(0xFF606366));

				setIfAbsent(this.obfuscated, new SerializableColor(0x4DFF5555));
				setIfAbsent(this.obfuscatedOutline, new SerializableColor(0x80FF5555));

				setIfAbsent(this.proposed, new SerializableColor(0x4D606366));
				setIfAbsent(this.proposedOutline, new SerializableColor(0x80606366));

				setIfAbsent(this.deobfuscated, new SerializableColor(0x4D50FA7B));
				setIfAbsent(this.deobfuscatedOutline, new SerializableColor(0x50FA7B));

				setIfAbsent(this.editorBackground, new SerializableColor(0xFF282A36));
				setIfAbsent(this.highlight, new SerializableColor(0xFFFF79C6));
				setIfAbsent(this.caret, new SerializableColor(0xFFF8F8F2));
				setIfAbsent(this.selectionHighlight, new SerializableColor(0xFFF8F8F2));
				setIfAbsent(this.string, new SerializableColor(0xFFF1FA8C));
				setIfAbsent(this.number, new SerializableColor(0xFFBD93F9));
				setIfAbsent(this.operator, new SerializableColor(0xFFF8F8F2));
				setIfAbsent(this.delimiter, new SerializableColor(0xFFF8F8F2));
				setIfAbsent(this.type, new SerializableColor(0xFFF8F8F2));
				setIfAbsent(this.identifier, new SerializableColor(0xFFF8F8F2));
				setIfAbsent(this.comment, new SerializableColor(0xFF339933));
				setIfAbsent(this.text, new SerializableColor(0xFFF8F8F2));

				setIfAbsent(this.debugToken, new SerializableColor(0x804B1370));
				setIfAbsent(this.debugTokenOutline, new SerializableColor(0x80701367));
			} else {
				resetIfAbsent(this.lineNumbersForeground);
				resetIfAbsent(this.lineNumbersBackground);
				resetIfAbsent(this.lineNumbersSelected);

				resetIfAbsent(this.obfuscated);
				resetIfAbsent(this.obfuscatedOutline);

				resetIfAbsent(this.proposed);
				resetIfAbsent(this.proposedOutline);

				resetIfAbsent(this.deobfuscated);
				resetIfAbsent(this.deobfuscatedOutline);

				resetIfAbsent(this.editorBackground);
				resetIfAbsent(this.highlight);
				resetIfAbsent(this.caret);
				resetIfAbsent(this.selectionHighlight);
				resetIfAbsent(this.string);
				resetIfAbsent(this.number);
				resetIfAbsent(this.operator);
				resetIfAbsent(this.delimiter);
				resetIfAbsent(this.type);
				resetIfAbsent(this.identifier);
				resetIfAbsent(this.text);

				resetIfAbsent(this.debugToken);
				resetIfAbsent(this.debugTokenOutline);
			}
		}

		private static <T> void resetIfAbsent(TrackedValue<T> value) {
			setIfAbsent(value, value.getDefaultValue());
		}

		private static <T> void setIfAbsent(TrackedValue<T> value, T newValue) {
			if (value.getDefaultValue().equals(value.value())) {
				value.setValue(newValue, true);
			}
		}
	}
}

package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.TrackedValue;

import java.awt.Color;

public class ThemeColors extends ReflectiveConfig.Section {
	public final TrackedValue<Color> lineNumbersForeground = this.value(new Color(0xFF333300, true));
	public final TrackedValue<Color> lineNumbersBackground = this.value(new Color(0xFFEEEEFF, true));
	public final TrackedValue<Color> lineNumbersSelected = this.value(new Color(0xFFCCCCEE, true));

	public final TrackedValue<Color> obfuscated = this.value(new Color(0xFFFFDCDC, true));
	public final TrackedValue<Color> obfuscatedOutline = this.value(new Color(0xFFA05050, true));

	public final TrackedValue<Color> proposed = this.value(new Color(0xFF000000, true));
	public final TrackedValue<Color> proposedOutline = this.value(new Color(0xBF000000, true));

	public final TrackedValue<Color> deobfuscated = this.value(new Color(0xFFDCFFDC, true));
	public final TrackedValue<Color> deobfuscatedOutline = this.value(new Color(0xFF50A050, true));

	public final TrackedValue<Color> editorBackground = this.value(new Color(0xFF50A050, true));
	public final TrackedValue<Color> highlight = this.value(new Color(0xFF50A050, true));
	public final TrackedValue<Color> caret = this.value(new Color(0xFF50A050, true));
	public final TrackedValue<Color> selectionHighlight = this.value(new Color(0xFF50A050, true));
	public final TrackedValue<Color> string = this.value(new Color(0xFFCC6600, true));
	public final TrackedValue<Color> number = this.value(new Color(0xFF999933, true));
	public final TrackedValue<Color> operator = this.value(new Color(0xFF000000, true));
	public final TrackedValue<Color> delimiter = this.value(new Color(0xFF000000, true));
	public final TrackedValue<Color> type = this.value(new Color(0xFF000000, true));
	public final TrackedValue<Color> identifier = this.value(new Color(0xFF000000, true));
	public final TrackedValue<Color> comment = this.value(new Color(0xFF339933, true));
	public final TrackedValue<Color> text = this.value(new Color(0xFF000000, true));

	public final TrackedValue<Color> debugToken = this.value(new Color(0xFFD9BEF9, true));
	public final TrackedValue<Color> debugTokenOutline = this.value(new Color(0xFFBD93F9, true));

	public final TrackedValue<Color> dockHighlight = this.value(new Color(0xFF0000FF, true));

	public void configure(boolean dark) {
		if (dark) {
			setIfAbsent(this.lineNumbersForeground, new Color(0xFFA4A4A3, true));
			setIfAbsent(this.lineNumbersBackground, new Color(0xFF313335, true));
			setIfAbsent(this.lineNumbersSelected, new Color(0xFF606366, true));

			setIfAbsent(this.obfuscated, new Color(0x4DFF5555, true));
			setIfAbsent(this.obfuscatedOutline, new Color(0x80FF5555, true));

			setIfAbsent(this.proposed, new Color(0x4D606366, true));
			setIfAbsent(this.proposedOutline, new Color(0x80606366, true));

			setIfAbsent(this.deobfuscated, new Color(0x4D50FA7B, true));
			setIfAbsent(this.deobfuscatedOutline, new Color(0x50FA7B, true));

			setIfAbsent(this.editorBackground, new Color(0xFF282A36, true));
			setIfAbsent(this.highlight, new Color(0xFFFF79C6, true));
			setIfAbsent(this.caret, new Color(0xFFF8F8F2, true));
			setIfAbsent(this.selectionHighlight, new Color(0xFFF8F8F2));
			setIfAbsent(this.string, new Color(0xFFF1FA8C, true));
			setIfAbsent(this.number, new Color(0xFFBD93F9, true));
			setIfAbsent(this.operator, new Color(0xFFF8F8F2, true));
			setIfAbsent(this.delimiter, new Color(0xFFF8F8F2, true));
			setIfAbsent(this.type, new Color(0xFFF8F8F2, true));
			setIfAbsent(this.identifier, new Color(0xFFF8F8F2, true));
			setIfAbsent(this.comment, new Color(0xFF339933, true));
			setIfAbsent(this.text, new Color(0xFFF8F8F2, true));

			setIfAbsent(this.debugToken, new Color(0x804B1370, true));
			setIfAbsent(this.debugTokenOutline, new Color(0x80701367, true));
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

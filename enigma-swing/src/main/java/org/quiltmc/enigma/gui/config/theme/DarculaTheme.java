package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.enigma.gui.config.theme.Theme.Colors.SerializableColor;

public class DarculaTheme extends Theme {
	public DarculaTheme(LookAndFeel lookAndFeel) {
		super(lookAndFeel);
	}

	@Override
	protected Colors.Builder buildDefaultColors(Colors.Builder colors) {
		return colors
			.lineNumbersForeground(new SerializableColor(0xFFA4A4A3))
			.lineNumbersBackground(new SerializableColor(0xFF313335))
			.lineNumbersSelected(new SerializableColor(0xFF606366))

			.obfuscated(new SerializableColor(0x4DFF5555))
			.obfuscatedOutline(new SerializableColor(0x80FF5555))

			.proposed(new SerializableColor(0x4D606366))
			.proposedOutline(new SerializableColor(0x80606366))

			.deobfuscated(new SerializableColor(0x4D50FA7B))
			.deobfuscatedOutline(new SerializableColor(0x8050FA7B))

			.editorBackground(new SerializableColor(0xFF282A36))
			.highlight(new SerializableColor(0xFFFF79C6))
			.caret(new SerializableColor(0xFFF8F8F2))
			.selectionHighlight(new SerializableColor(0xFFF8F8F2))
			.string(new SerializableColor(0xFFF1FA8C))
			.number(new SerializableColor(0xFFBD93F9))
			.operator(new SerializableColor(0xFFF8F8F2))
			.delimiter(new SerializableColor(0xFFF8F8F2))
			.type(new SerializableColor(0xFFF8F8F2))
			.identifier(new SerializableColor(0xFFF8F8F2))
			.comment(new SerializableColor(0xFF339933))
			.text(new SerializableColor(0xFFF8F8F2))

			.debugToken(new SerializableColor(0x804B1370))
			.debugTokenOutline(new SerializableColor(0x80701367));
	}
}

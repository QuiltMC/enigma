package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.enigma.gui.config.theme.Theme.Colors.SerializableColor;

public class DarcerulaTheme extends Theme {
	public DarcerulaTheme(LookAndFeel lookAndFeel) {
		super(lookAndFeel);
	}

	@Override
	protected Colors.Builder buildDefaultColors(Colors.Builder colors) {
		return colors
			.lineNumbersForeground(new SerializableColor(0xFFDBDBDA))
			.lineNumbersBackground(new SerializableColor(0xFF252729))
			.lineNumbersSelected(new SerializableColor(0xFF353739))

			.obfuscated(new SerializableColor(0x31FF5555))
			.obfuscatedOutline(new SerializableColor(0x89FF5555))

			.proposed(new SerializableColor(0x43606366))
			.proposedOutline(new SerializableColor(0x86606366))

			.deobfuscated(new SerializableColor(0x2450FA7B))
			.deobfuscatedOutline(new SerializableColor(0x8050FA7B))

			.editorBackground(new SerializableColor(0xFF1B1B20))
			.highlight(new SerializableColor(0xFFFF8BD8))
			.caret(new SerializableColor(0xFFF8F8F2))
			.selectionHighlight(new SerializableColor(0xFFF8F8F2))
			.string(new SerializableColor(0xFFF1FA8C))
			.number(new SerializableColor(0xFFD5ABFF))
			.operator(new SerializableColor(0xFFF8F8F2))
			.delimiter(new SerializableColor(0xFFFF5555))
			.type(new SerializableColor(0xFFF8F8F2))
			.identifier(new SerializableColor(0xFFF8F8F2))
			.comment(new SerializableColor(0xFF63C963))
			.text(new SerializableColor(0xFFF8F8F2))

			.debugToken(new SerializableColor(0x804B1370))
			.debugTokenOutline(new SerializableColor(0x80701367));
	}
}

package org.quiltmc.enigma.gui.config.theme;

/**
 * <p>
 * Factory  methods for creating non-default sets of colors for themes.
 * <p>
 * These can't be created in {@link Theme} sub-classes because of a quilt-config limitation.
 */
public final class ColorsFactories {
	private ColorsFactories() { }

	public static Theme.Colors.Builder createDarcula() {
		final var colors = new Theme.Colors.Builder();
		return colors
			.lineNumbersForeground(new Theme.Colors.SerializableColor(0xFFA4A4A3))
			.lineNumbersBackground(new Theme.Colors.SerializableColor(0xFF313335))
			.lineNumbersSelected(new Theme.Colors.SerializableColor(0xFF606366))

			.obfuscated(new Theme.Colors.SerializableColor(0x4DFF5555))
			.obfuscatedOutline(new Theme.Colors.SerializableColor(0x80FF5555))

			.proposed(new Theme.Colors.SerializableColor(0x4D606366))
			.proposedOutline(new Theme.Colors.SerializableColor(0x80606366))

			.deobfuscated(new Theme.Colors.SerializableColor(0x4D50FA7B))
			.deobfuscatedOutline(new Theme.Colors.SerializableColor(0x8050FA7B))

			.editorBackground(new Theme.Colors.SerializableColor(0xFF282A36))
			.highlight(new Theme.Colors.SerializableColor(0xFFFF79C6))
			.caret(new Theme.Colors.SerializableColor(0xFFF8F8F2))
			.selectionHighlight(new Theme.Colors.SerializableColor(0xFFF8F8F2))
			.string(new Theme.Colors.SerializableColor(0xFFF1FA8C))
			.number(new Theme.Colors.SerializableColor(0xFFBD93F9))
			.operator(new Theme.Colors.SerializableColor(0xFFF8F8F2))
			.delimiter(new Theme.Colors.SerializableColor(0xFFF8F8F2))
			.type(new Theme.Colors.SerializableColor(0xFFF8F8F2))
			.identifier(new Theme.Colors.SerializableColor(0xFFF8F8F2))
			.comment(new Theme.Colors.SerializableColor(0xFF339933))
			.text(new Theme.Colors.SerializableColor(0xFFF8F8F2))

			.debugToken(new Theme.Colors.SerializableColor(0x804B1370))
			.debugTokenOutline(new Theme.Colors.SerializableColor(0x80701367));
	}

	public static Theme.Colors.Builder createDarcerula() {
		return createDarcula()
			.lineNumbersForeground(new Theme.Colors.SerializableColor(0xFFDBDBDA))
			.lineNumbersBackground(new Theme.Colors.SerializableColor(0xFF252729))
			.lineNumbersSelected(new Theme.Colors.SerializableColor(0xFF353739))

			.obfuscated(new Theme.Colors.SerializableColor(0x31FF5555))
			.obfuscatedOutline(new Theme.Colors.SerializableColor(0x89FF5555))

			.proposed(new Theme.Colors.SerializableColor(0x43606366))
			.proposedOutline(new Theme.Colors.SerializableColor(0x86606366))

			.deobfuscated(new Theme.Colors.SerializableColor(0x2450FA7B))
			// deobfuscatedOutline inherited from darcula

			.editorBackground(new Theme.Colors.SerializableColor(0xFF1B1B20))
			.highlight(new Theme.Colors.SerializableColor(0xFFFF8BD8))
			// caret inherited from darcula
			// selectionHighlight inherited from darcula
			// string inherited from darcula
			.number(new Theme.Colors.SerializableColor(0xFFD5ABFF))
			// operator inherited from darcula
			.delimiter(new Theme.Colors.SerializableColor(0xFFFF5555))
			// type inherited from darcula
			// identifier inherited from darcula
			.comment(new Theme.Colors.SerializableColor(0xFF63C963));
			// text inherited from darcula

			// debugToken inherited from darcula
			// debugTokenOutline inherited from darcula
	}
}

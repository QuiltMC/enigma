package org.quiltmc.enigma.gui.config.theme;

/**
 * Factory  methods for creating syntax pane colors for themes.
 *
 * <p>
 * These can't be created in {@link ThemeCreator} subclasses because of a quilt-config limitation.
 */
public final class SyntaxPaneColorsFactories {
	private SyntaxPaneColorsFactories() { }

	public static ThemeCreator.SyntaxPaneColors.Builder createLight() {
		// default colors are for LookAndFeel.DEFAULT
		return new ThemeCreator.SyntaxPaneColors.Builder();
	}

	public static ThemeCreator.SyntaxPaneColors.Builder createDarcula() {
		return new ThemeCreator.SyntaxPaneColors.Builder()
			.lineNumbersForeground(new ThemeCreator.SerializableColor(0xFFA4A4A3))
			.lineNumbersBackground(new ThemeCreator.SerializableColor(0xFF313335))
			.lineNumbersSelected(new ThemeCreator.SerializableColor(0xFF606366))

			.obfuscated(new ThemeCreator.SerializableColor(0x4DFF5555))
			.obfuscatedOutline(new ThemeCreator.SerializableColor(0x80FF5555))

			.proposed(new ThemeCreator.SerializableColor(0x4D606366))
			.proposedOutline(new ThemeCreator.SerializableColor(0x80606366))

			.deobfuscated(new ThemeCreator.SerializableColor(0x4D50FA7B))
			.deobfuscatedOutline(new ThemeCreator.SerializableColor(0x8050FA7B))

			.editorBackground(new ThemeCreator.SerializableColor(0xFF282A36))
			.highlight(new ThemeCreator.SerializableColor(0xFFFF79C6))
			.caret(new ThemeCreator.SerializableColor(0xFFF8F8F2))
			.selectionHighlight(new ThemeCreator.SerializableColor(0xFFF8F8F2))
			.string(new ThemeCreator.SerializableColor(0xFFF1FA8C))
			.number(new ThemeCreator.SerializableColor(0xFFBD93F9))
			.operator(new ThemeCreator.SerializableColor(0xFFF8F8F2))
			.delimiter(new ThemeCreator.SerializableColor(0xFFF8F8F2))
			.type(new ThemeCreator.SerializableColor(0xFFF8F8F2))
			.identifier(new ThemeCreator.SerializableColor(0xFFF8F8F2))
			.comment(new ThemeCreator.SerializableColor(0xFF339933))
			.text(new ThemeCreator.SerializableColor(0xFFF8F8F2))

			.debugToken(new ThemeCreator.SerializableColor(0x804B1370))
			.debugTokenOutline(new ThemeCreator.SerializableColor(0x80701367));
	}

	public static ThemeCreator.SyntaxPaneColors.Builder createDarcerula() {
		return createDarcula()
			.lineNumbersForeground(new ThemeCreator.SerializableColor(0xFFDBDBDA))
			.lineNumbersBackground(new ThemeCreator.SerializableColor(0xFF252729))
			.lineNumbersSelected(new ThemeCreator.SerializableColor(0xFF353739))

			.obfuscated(new ThemeCreator.SerializableColor(0x31FF5555))
			.obfuscatedOutline(new ThemeCreator.SerializableColor(0x89FF5555))

			.proposed(new ThemeCreator.SerializableColor(0x43606366))
			.proposedOutline(new ThemeCreator.SerializableColor(0x86606366))

			.deobfuscated(new ThemeCreator.SerializableColor(0x2450FA7B))
			// deobfuscatedOutline inherited from darcula

			.editorBackground(new ThemeCreator.SerializableColor(0xFF1B1B20))
			.highlight(new ThemeCreator.SerializableColor(0xFFFF8BD8))
			// caret inherited from darcula
			// selectionHighlight inherited from darcula
			// string inherited from darcula
			.number(new ThemeCreator.SerializableColor(0xFFD5ABFF))
			// operator inherited from darcula
			.delimiter(new ThemeCreator.SerializableColor(0xFFFF5555))
			// type inherited from darcula
			// identifier inherited from darcula
			.comment(new ThemeCreator.SerializableColor(0xFF63C963));
			// text inherited from darcula

			// debugToken inherited from darcula
			// debugTokenOutline inherited from darcula
	}
}

package org.quiltmc.enigma.gui.config.theme;

/**
 * Factory  methods for creating syntax pane colors for themes.
 *
 * <p>
 * These can't be created in {@link Theme} subclasses because of a quilt-config limitation.
 */
public final class SyntaxPaneColorsFactories {
	private SyntaxPaneColorsFactories() { }

	public static Theme.SyntaxPaneColors.Builder createLight() {
		// default colors are for LookAndFeel.DEFAULT
		return new Theme.SyntaxPaneColors.Builder();
	}

	public static Theme.SyntaxPaneColors.Builder createDarcula() {
		return new Theme.SyntaxPaneColors.Builder()
			.lineNumbersForeground(new Theme.SerializableColor(0xFFA4A4A3))
			.lineNumbersBackground(new Theme.SerializableColor(0xFF313335))
			.lineNumbersSelected(new Theme.SerializableColor(0xFF606366))

			.obfuscated(new Theme.SerializableColor(0x4DFF5555))
			.obfuscatedOutline(new Theme.SerializableColor(0x80FF5555))

			.proposed(new Theme.SerializableColor(0x4D606366))
			.proposedOutline(new Theme.SerializableColor(0x80606366))

			.deobfuscated(new Theme.SerializableColor(0x4D50FA7B))
			.deobfuscatedOutline(new Theme.SerializableColor(0x8050FA7B))

			.editorBackground(new Theme.SerializableColor(0xFF282A36))
			.highlight(new Theme.SerializableColor(0xFFFF79C6))
			.caret(new Theme.SerializableColor(0xFFF8F8F2))
			.selectionHighlight(new Theme.SerializableColor(0xFFF8F8F2))
			.string(new Theme.SerializableColor(0xFFF1FA8C))
			.number(new Theme.SerializableColor(0xFFBD93F9))
			.operator(new Theme.SerializableColor(0xFFF8F8F2))
			.delimiter(new Theme.SerializableColor(0xFFF8F8F2))
			.type(new Theme.SerializableColor(0xFFF8F8F2))
			.identifier(new Theme.SerializableColor(0xFFF8F8F2))
			.comment(new Theme.SerializableColor(0xFF339933))
			.text(new Theme.SerializableColor(0xFFF8F8F2))

			.debugToken(new Theme.SerializableColor(0x804B1370))
			.debugTokenOutline(new Theme.SerializableColor(0x80701367));
	}

	public static Theme.SyntaxPaneColors.Builder createDarcerula() {
		return createDarcula()
			.lineNumbersForeground(new Theme.SerializableColor(0xFFDBDBDA))
			.lineNumbersBackground(new Theme.SerializableColor(0xFF252729))
			.lineNumbersSelected(new Theme.SerializableColor(0xFF353739))

			.obfuscated(new Theme.SerializableColor(0x31FF5555))
			.obfuscatedOutline(new Theme.SerializableColor(0x89FF5555))

			.proposed(new Theme.SerializableColor(0x43606366))
			.proposedOutline(new Theme.SerializableColor(0x86606366))

			.deobfuscated(new Theme.SerializableColor(0x2450FA7B))
			// deobfuscatedOutline inherited from darcula

			.editorBackground(new Theme.SerializableColor(0xFF1B1B20))
			.highlight(new Theme.SerializableColor(0xFFFF8BD8))
			// caret inherited from darcula
			// selectionHighlight inherited from darcula
			// string inherited from darcula
			.number(new Theme.SerializableColor(0xFFD5ABFF))
			// operator inherited from darcula
			.delimiter(new Theme.SerializableColor(0xFFFF5555))
			// type inherited from darcula
			// identifier inherited from darcula
			.comment(new Theme.SerializableColor(0xFF63C963));
			// text inherited from darcula

			// debugToken inherited from darcula
			// debugTokenOutline inherited from darcula
	}
}

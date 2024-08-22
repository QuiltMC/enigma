package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.enigma.gui.config.theme.properties.ThemeProperties;

/**
 * Factory  methods for creating syntax pane colors for themes.
 *
 * <p>
 * These can't be created in {@link ThemeProperties} subclasses because of a quilt-config limitation.
 */
public final class SyntaxPaneColorsFactories {
	private SyntaxPaneColorsFactories() { }

	public static ThemeProperties.SyntaxPaneColors.Builder createLight() {
		// default colors are for LookAndFeel.DEFAULT
		return new ThemeProperties.SyntaxPaneColors.Builder();
	}

	public static ThemeProperties.SyntaxPaneColors.Builder createDarcula() {
		return new ThemeProperties.SyntaxPaneColors.Builder()
			.lineNumbersForeground(new ThemeProperties.SerializableColor(0xFFA4A4A3))
			.lineNumbersBackground(new ThemeProperties.SerializableColor(0xFF313335))
			.lineNumbersSelected(new ThemeProperties.SerializableColor(0xFF606366))

			.obfuscated(new ThemeProperties.SerializableColor(0x4DFF5555))
			.obfuscatedOutline(new ThemeProperties.SerializableColor(0x80FF5555))

			.proposed(new ThemeProperties.SerializableColor(0x4D606366))
			.proposedOutline(new ThemeProperties.SerializableColor(0x80606366))

			.deobfuscated(new ThemeProperties.SerializableColor(0x4D50FA7B))
			.deobfuscatedOutline(new ThemeProperties.SerializableColor(0x8050FA7B))

			.editorBackground(new ThemeProperties.SerializableColor(0xFF282A36))
			.highlight(new ThemeProperties.SerializableColor(0xFFFF79C6))
			.caret(new ThemeProperties.SerializableColor(0xFFF8F8F2))
			.selectionHighlight(new ThemeProperties.SerializableColor(0xFFF8F8F2))
			.string(new ThemeProperties.SerializableColor(0xFFF1FA8C))
			.number(new ThemeProperties.SerializableColor(0xFFBD93F9))
			.operator(new ThemeProperties.SerializableColor(0xFFF8F8F2))
			.delimiter(new ThemeProperties.SerializableColor(0xFFF8F8F2))
			.type(new ThemeProperties.SerializableColor(0xFFF8F8F2))
			.identifier(new ThemeProperties.SerializableColor(0xFFF8F8F2))
			.comment(new ThemeProperties.SerializableColor(0xFF339933))
			.text(new ThemeProperties.SerializableColor(0xFFF8F8F2))

			.debugToken(new ThemeProperties.SerializableColor(0x804B1370))
			.debugTokenOutline(new ThemeProperties.SerializableColor(0x80701367));
	}

	public static ThemeProperties.SyntaxPaneColors.Builder createDarcerula() {
		return createDarcula()
			.lineNumbersForeground(new ThemeProperties.SerializableColor(0xFFDBDBDA))
			.lineNumbersBackground(new ThemeProperties.SerializableColor(0xFF252729))
			.lineNumbersSelected(new ThemeProperties.SerializableColor(0xFF353739))

			.obfuscated(new ThemeProperties.SerializableColor(0x31FF5555))
			.obfuscatedOutline(new ThemeProperties.SerializableColor(0x89FF5555))

			.proposed(new ThemeProperties.SerializableColor(0x43606366))
			.proposedOutline(new ThemeProperties.SerializableColor(0x86606366))

			.deobfuscated(new ThemeProperties.SerializableColor(0x2450FA7B))
			// deobfuscatedOutline inherited from darcula

			.editorBackground(new ThemeProperties.SerializableColor(0xFF1B1B20))
			.highlight(new ThemeProperties.SerializableColor(0xFFFF8BD8))
			// caret inherited from darcula
			// selectionHighlight inherited from darcula
			// string inherited from darcula
			.number(new ThemeProperties.SerializableColor(0xFFD5ABFF))
			// operator inherited from darcula
			.delimiter(new ThemeProperties.SerializableColor(0xFFFF5555))
			// type inherited from darcula
			// identifier inherited from darcula
			.comment(new ThemeProperties.SerializableColor(0xFF63C963));
			// text inherited from darcula

			// debugToken inherited from darcula
			// debugTokenOutline inherited from darcula
	}
}

package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.ThemeChoice;
import org.quiltmc.enigma.gui.config.theme.properties.composite.ConfigurableConfigCreator;

import java.util.ArrayList;
import java.util.List;

public class DarculaThemeProperties extends AbstractDarculaThemeProperties {
	public DarculaThemeProperties() {
		this(new SyntaxPaneProperties(), new LookAndFeelProperties(), new ArrayList<>());
	}

	protected DarculaThemeProperties(org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties syntaxPaneColors, org.quiltmc.enigma.gui.config.theme.properties.composite.LookAndFeelProperties lookAndFeelColors, List<ConfigurableConfigCreator> creators) {
		super(syntaxPaneColors, lookAndFeelColors, creators);
	}

	@Override
	public ThemeChoice getThemeChoice() {
		return ThemeChoice.DARCULA;
	}

	protected static class LookAndFeelProperties extends
		org.quiltmc.enigma.gui.config.theme.properties.composite.LookAndFeelProperties {
		@Override
		protected Colors.Builder buildLookAndFeelColors (Colors.Builder lookAndFeelColors){
			// colors are from FlatDarkLaf.properties
			return lookAndFeelColors
				.foreground(new SerializableColor(0xFFBBBBBB))
				.background(new SerializableColor(0xFF3C3F41))

				.accentBaseColor(new SerializableColor(0xFF4B6EAF))

				.activeCaption(new SerializableColor(0xFF434E60))
				.inactiveCaption(new SerializableColor(0xFF393C3D))

				.errorBorder(new SerializableColor(0xFF8B3C3C))
				.warningBorder(new SerializableColor(0xFFAC7920));
		}
	}

	protected static class SyntaxPaneProperties extends
		org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties {
		@Override
		public Colors.Builder buildSyntaxPaneColors(Colors.Builder syntaxPaneColors) {
			return syntaxPaneColors
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
}

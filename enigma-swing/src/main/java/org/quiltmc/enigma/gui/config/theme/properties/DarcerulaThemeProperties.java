package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.enigma.gui.config.theme.properties.composite.ConfigurableConfigCreator;

import java.util.ArrayList;
import java.util.List;

public class DarcerulaThemeProperties extends AbstractDarculaThemeProperties {
	public DarcerulaThemeProperties() {
		this(new SyntaxPaneProperties(), new LookAndFeelProperties(), new ArrayList<>());
	}

	protected DarcerulaThemeProperties(org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties syntaxPaneColors, org.quiltmc.enigma.gui.config.theme.properties.composite.LookAndFeelProperties lookAndFeelColors, List<ConfigurableConfigCreator> creators) {
		super(syntaxPaneColors, lookAndFeelColors, creators);
	}

	public static class SyntaxPaneProperties extends DarculaThemeProperties.SyntaxPaneProperties {
		@Override
		public Colors.Builder buildSyntaxPaneColors(Colors.Builder syntaxPaneColors) {
			return super.buildSyntaxPaneColors(syntaxPaneColors)
				.lineNumbersForeground(new SerializableColor(0xFFDBDBDA))
				.lineNumbersBackground(new SerializableColor(0xFF252729))
				.lineNumbersSelected(new SerializableColor(0xFF353739))

				.obfuscated(new SerializableColor(0x31FF5555))
				.obfuscatedOutline(new SerializableColor(0x89FF5555))

				.proposed(new SerializableColor(0x43606366))
				.proposedOutline(new SerializableColor(0x86606366))

				.deobfuscated(new SerializableColor(0x2450FA7B))
				// deobfuscatedOutline inherited from darcula

				.editorBackground(new SerializableColor(0xFF1B1B20))
				.highlight(new SerializableColor(0xFFFF8BD8))
				// caret inherited from darcula
				// selectionHighlight inherited from darcula
				// string inherited from darcula
				.number(new SerializableColor(0xFFD5ABFF))
				// operator inherited from darcula
				.delimiter(new SerializableColor(0xFFFF5555))
				// type inherited from darcula
				// identifier inherited from darcula
				.comment(new SerializableColor(0xFF63C963));
				// text inherited from darcula

				// debugToken inherited from darcula
				// debugTokenOutline inherited from darcula
			}
	}

	protected static class LookAndFeelProperties extends DarculaThemeProperties.LookAndFeelProperties {
		@Override
		protected Colors.Builder buildLookAndFeelColors(Colors.Builder lookAndFeelColors) {
			return super.buildLookAndFeelColors(lookAndFeelColors)
				.foreground(new SerializableColor(0xFFC3C3C3))
				.background(new SerializableColor(0xFF242729))

				.accentBaseColor(new SerializableColor(0xFF4366A7))

				.activeCaption(new SerializableColor(0xFF374254))
				.inactiveCaption(new SerializableColor(0xFF2B2E2F));

				// errorBorder inherited from darcula
				// warningBorder inherited from darcula
		}
	}
}

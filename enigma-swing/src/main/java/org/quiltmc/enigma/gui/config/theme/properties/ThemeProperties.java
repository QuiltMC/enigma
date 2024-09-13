package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.config.api.Config;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.values.ComplexConfigValue;
import org.quiltmc.config.api.values.ConfigSerializableObject;
import org.quiltmc.enigma.gui.config.theme.properties.composite.CompositeConfigCreator;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;
import org.quiltmc.enigma.gui.util.ListUtil;

import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Color;
import java.util.List;

public abstract class ThemeProperties extends CompositeConfigCreator {
	private final SyntaxPaneProperties syntaxPaneProperties;

	protected ThemeProperties(
			SyntaxPaneProperties syntaxPaneColors,
			List<Config.Creator> creators
	) {
		super(ListUtil.prepend(syntaxPaneColors, creators));
		this.syntaxPaneProperties = syntaxPaneColors;
	}

	public final SyntaxPaneProperties.Colors getSyntaxPaneColors() {
		return this.syntaxPaneProperties.colors;
	}

	public abstract void setGlobalLaf() throws
			UnsupportedLookAndFeelException, ClassNotFoundException,
			InstantiationException, IllegalAccessException;

	// FlatLaf-based LaFs do their own scaling so we don't have to do it.
	// Running swing-dpi for FlatLaf actually breaks fonts, so we let it scale the GUI.
	public abstract boolean onlyScaleFonts();

	public static class SerializableColor extends Color implements ConfigSerializableObject<String> {
		public static void addFormatComment(Comment.Builder comment) {
			comment.add("Colors are encoded in the RGBA format.");
		}

		public SerializableColor(int rgba) {
			super(rgba, true);
		}

		@Override
		public SerializableColor convertFrom(String representation) {
			return new SerializableColor(new Color(
				Integer.valueOf(representation.substring(0, 2), 16),
				Integer.valueOf(representation.substring(2, 4), 16),
				Integer.valueOf(representation.substring(4, 6), 16),
				Integer.valueOf(representation.substring(6, 8), 16)
			).getRGB());
		}

		@Override
		public String getRepresentation() {
			int rgba = (this.getRGB() << 8) | this.getAlpha();
			return String.format("%08X", rgba);
		}

		@Override
		public ComplexConfigValue copy() {
			return new SerializableColor(this.getRGB());
		}

		@Override
		public String toString() {
			return "SerializableColor" + "[r=" + this.getRed() + ",g=" + this.getGreen() + ",b=" + this.getBlue() + ",a=" + this.getAlpha() + "]" + " (hex=" + this.getRepresentation() + ")";
		}
	}
}

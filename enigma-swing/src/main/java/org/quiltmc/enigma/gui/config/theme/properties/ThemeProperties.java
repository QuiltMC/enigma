package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.values.ComplexConfigValue;
import org.quiltmc.config.api.values.ConfigSerializableObject;
import org.quiltmc.enigma.gui.config.theme.ThemeChoice;
import org.quiltmc.enigma.gui.config.theme.properties.composite.CompositeConfigCreator;
import org.quiltmc.enigma.gui.config.theme.properties.composite.ConfigurableConfigCreator;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;
import org.quiltmc.enigma.gui.util.ListUtil;

import javax.swing.*;
import java.awt.Color;
import java.util.List;

public abstract class ThemeProperties extends CompositeConfigCreator {
	public static void addColorFormatComment(Comment.Builder builder) {
		builder.add("Colors are encoded in the RGBA format.");
	}

	public final ThemeChoice choice;

	private final SyntaxPaneProperties syntaxPaneProperties;

	protected ThemeProperties(SyntaxPaneProperties syntaxPaneColors, List<ConfigurableConfigCreator> creators) {
		super(ListUtil.prepend(syntaxPaneColors, creators));
		this.choice = this.getThemeChoice();
		this.syntaxPaneProperties = syntaxPaneColors;
	}

	public abstract ThemeChoice getThemeChoice();

	public final SyntaxPaneProperties.Colors getSyntaxPaneColors() {
		return this.syntaxPaneProperties.colors;
	}

	public abstract void setGlobalLaf() throws
			UnsupportedLookAndFeelException, ClassNotFoundException,
			InstantiationException, IllegalAccessException;


	public static class SerializableColor extends Color implements ConfigSerializableObject<String> {
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

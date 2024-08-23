package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.ComplexConfigValue;
import org.quiltmc.config.api.values.ConfigSerializableObject;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueMap;
import org.quiltmc.config.implementor_api.ConfigEnvironment;
import org.quiltmc.config.implementor_api.ConfigFactory;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;
import org.quiltmc.enigma.gui.config.theme.properties.ThemeProperties;

import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Font;

public class Theme extends ReflectiveConfig {
	public static Theme create(ConfigEnvironment environment, String family, String id, ThemeProperties properties) {
		final Theme theme = ConfigFactory.create(environment, family, id, properties, Theme.class);

		// set properties here because ConfigFactory.create(...) requires a no-args constructor
		theme.properties = properties;

		return theme;
	}

	private transient ThemeProperties properties;

	public final Fonts fonts = new Fonts();

	public SyntaxPaneProperties.Colors getSyntaxPaneColors() {
		return this.properties.getSyntaxPaneColors();
	}

	public void setGlobalLaf() throws
			UnsupportedLookAndFeelException, ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		this.properties.setGlobalLaf();
	}

	public void configure() {
		this.properties.configure();
	}

	public boolean needsScaling() {
		return this.properties.needsScaling();
	}

	public static class Fonts extends Section {
		public final TrackedValue<SerializableFont> defaultBold = this.value(new SerializableFont(Font.decode(Font.DIALOG).deriveFont(Font.BOLD)));
		public final TrackedValue<SerializableFont> defaultNormal = this.value(new SerializableFont(Font.decode(Font.DIALOG)));
		public final TrackedValue<SerializableFont> small = this.value(new SerializableFont(Font.decode(Font.DIALOG)));
		public final TrackedValue<SerializableFont> editor = this.value(new SerializableFont(Font.decode(Font.MONOSPACED).deriveFont(12f)));

		public static class SerializableFont extends Font implements ConfigSerializableObject<ValueMap<String>> {
			public SerializableFont(Font font) {
				this(font.getName(), font.getStyle(), font.getSize());
			}

			public SerializableFont(String name, int style, int size) {
				super(name, style, size);
			}

			@Override
			public ConfigSerializableObject<ValueMap<String>> convertFrom(ValueMap<String> representation) {
				return new SerializableFont(
					representation.get("name"),
					Integer.parseInt(representation.get("style")),
					Integer.parseInt(representation.get("size"))
				);
			}

			@Override
			public ValueMap<String> getRepresentation() {
				return ValueMap.builder("")
					.put("name", this.name)
					.put("style", String.valueOf(this.style))
					.put("size", String.valueOf(this.size))
					.build();
			}

			@Override
			public ComplexConfigValue copy() {
				return new SerializableFont(this.name, this.style, this.size);
			}

			@Override
			public String toString() {
				return "SerializableFont" + "[name=" + this.name + ",style=" + this.style + ",size=" + this.size + "]";
			}
		}
	}
}

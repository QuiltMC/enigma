package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.ComplexConfigValue;
import org.quiltmc.config.api.values.ConfigSerializableObject;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueMap;

import java.awt.Color;
import java.awt.Font;
import java.util.stream.Stream;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class Theme extends ReflectiveConfig.Section {
	private static <T> void resetIfAbsent(TrackedValue<T> value) {
		setIfAbsent(value, value.getDefaultValue());
	}

	private static <T> void setIfAbsent(TrackedValue<T> value, T newValue) {
		if (value.getDefaultValue().equals(value.value())) {
			value.setValue(newValue, true);
		}
	}

	public final transient ThemeProperties properties;

	@Comment("Colors are encoded in the RGBA format.")
	public final SyntaxPaneColors syntaxPaneColors;

	@Comment("Colors are encoded in the RGBA format.")
	public final LookAndFeelColors lookAndFeelColors;

	public final Fonts fonts = new Fonts();

	public Theme(ThemeProperties properties) {
		this.properties = properties;
		this.syntaxPaneColors = properties.syntaxPaneColorsFactory.get().build();
		this.lookAndFeelColors = properties.lookAndFeelColorsFactory.get().build();
	}

	public static class Fonts extends ReflectiveConfig.Section {
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

	/**
	 * Default values are for light themes.
	 */
	public static class SyntaxPaneColors extends ReflectiveConfig.Section {
		public final TrackedValue<SerializableColor> lineNumbersForeground;
		public final TrackedValue<SerializableColor> lineNumbersBackground;
		public final TrackedValue<SerializableColor> lineNumbersSelected;
		public final TrackedValue<SerializableColor> obfuscated;
		public final TrackedValue<SerializableColor> obfuscatedOutline;

		public final TrackedValue<SerializableColor> proposed;
		public final TrackedValue<SerializableColor> proposedOutline;

		public final TrackedValue<SerializableColor> deobfuscated;
		public final TrackedValue<SerializableColor> deobfuscatedOutline;

		public final TrackedValue<SerializableColor> editorBackground;
		public final TrackedValue<SerializableColor> highlight;
		public final TrackedValue<SerializableColor> caret;
		public final TrackedValue<SerializableColor> selectionHighlight;
		public final TrackedValue<SerializableColor> string;
		public final TrackedValue<SerializableColor> number;
		public final TrackedValue<SerializableColor> operator;
		public final TrackedValue<SerializableColor> delimiter;
		public final TrackedValue<SerializableColor> type;
		public final TrackedValue<SerializableColor> identifier;
		public final TrackedValue<SerializableColor> comment;
		public final TrackedValue<SerializableColor> text;
		public final TrackedValue<SerializableColor> debugToken;
		public final TrackedValue<SerializableColor> debugTokenOutline;
		public final TrackedValue<SerializableColor> dockHighlight;

		private SyntaxPaneColors(
				SerializableColor lineNumbersForeground,
				SerializableColor lineNumbersBackground,
				SerializableColor lineNumbersSelected,
				SerializableColor obfuscated,
				SerializableColor obfuscatedOutline,

				SerializableColor proposed,
				SerializableColor proposedOutline,

				SerializableColor deobfuscated,
				SerializableColor deobfuscatedOutline,

				SerializableColor editorBackground,
				SerializableColor highlight,
				SerializableColor caret,
				SerializableColor selectionHighlight,
				SerializableColor string,
				SerializableColor number,
				SerializableColor operator,
				SerializableColor delimiter,
				SerializableColor type,
				SerializableColor identifier,
				SerializableColor comment,
				SerializableColor text,
				SerializableColor debugToken,
				SerializableColor debugTokenOutline,
				SerializableColor dockHighlight
		) {
			this.lineNumbersForeground = this.value(lineNumbersForeground);
			this.lineNumbersBackground = this.value(lineNumbersBackground);
			this.lineNumbersSelected = this.value(lineNumbersSelected);
			this.obfuscated = this.value(obfuscated);
			this.obfuscatedOutline = this.value(obfuscatedOutline);

			this.proposed = this.value(proposed);
			this.proposedOutline = this.value(proposedOutline);

			this.deobfuscated = this.value(deobfuscated);
			this.deobfuscatedOutline = this.value(deobfuscatedOutline);

			this.editorBackground = this.value(editorBackground);
			this.highlight = this.value(highlight);
			this.caret = this.value(caret);
			this.selectionHighlight = this.value(selectionHighlight);
			this.string = this.value(string);
			this.number = this.value(number);
			this.operator = this.value(operator);
			this.delimiter = this.value(delimiter);
			this.type = this.value(type);
			this.identifier = this.value(identifier);
			this.comment = this.value(comment);
			this.text = this.value(text);
			this.debugToken = this.value(debugToken);
			this.debugTokenOutline = this.value(debugTokenOutline);
			this.dockHighlight = this.value(dockHighlight);
		}

		public void configure() {
			this.stream().forEach(Theme::resetIfAbsent);
		}

		public Stream<TrackedValue<SerializableColor>> stream() {
			return Stream.of(
				this.lineNumbersForeground,
				this.lineNumbersBackground,
				this.lineNumbersSelected,

				this.obfuscated,
				this.obfuscatedOutline,

				this.proposed,
				this.proposedOutline,

				this.deobfuscated,
				this.deobfuscatedOutline,

				this.editorBackground,
				this.highlight,
				this.caret,
				this.selectionHighlight,
				this.string,
				this.number,
				this.operator,
				this.delimiter,
				this.type,
				this.identifier,
				this.text,

				this.debugToken,
				this.debugTokenOutline
			);
		}

		public static class Builder {
			private SerializableColor lineNumbersForeground = new SerializableColor(0xFF333300);
			private SerializableColor lineNumbersBackground = new SerializableColor(0xFFEEEEFF);
			private SerializableColor lineNumbersSelected = new SerializableColor(0xFFCCCCEE);
			private SerializableColor obfuscated = new SerializableColor(0xFFFFDCDC);
			private SerializableColor obfuscatedOutline = new SerializableColor(0xFFA05050);

			private SerializableColor proposed = new SerializableColor(0x27000000);
			private SerializableColor proposedOutline = new SerializableColor(0xBF000000);

			private SerializableColor deobfuscated = new SerializableColor(0xFFDCFFDC);
			private SerializableColor deobfuscatedOutline = new SerializableColor(0xFF50A050);

			private SerializableColor editorBackground = new SerializableColor(0xFFFFFFFF);
			private SerializableColor highlight = new SerializableColor(0xFF3333EE);
			private SerializableColor caret = new SerializableColor(0xFF000000);
			private SerializableColor selectionHighlight = new SerializableColor(0xFF000000);
			private SerializableColor string = new SerializableColor(0xFFCC6600);
			private SerializableColor number = new SerializableColor(0xFF999933);
			private SerializableColor operator = new SerializableColor(0xFF000000);
			private SerializableColor delimiter = new SerializableColor(0xFF000000);
			private SerializableColor type = new SerializableColor(0xFF000000);
			private SerializableColor identifier = new SerializableColor(0xFF000000);
			private SerializableColor comment = new SerializableColor(0xFF339933);
			private SerializableColor text = new SerializableColor(0xFF000000);
			private SerializableColor debugToken = new SerializableColor(0xFFD9BEF9);
			private SerializableColor debugTokenOutline = new SerializableColor(0xFFBD93F9);
			private SerializableColor dockHighlight = new SerializableColor(0xFF0000FF);

			public SyntaxPaneColors build() {
				return new SyntaxPaneColors(
					this.lineNumbersForeground,
					this.lineNumbersBackground,
					this.lineNumbersSelected,
					this.obfuscated,
					this.obfuscatedOutline,

					this.proposed,
					this.proposedOutline,

					this.deobfuscated,
					this.deobfuscatedOutline,

					this.editorBackground,
					this.highlight,
					this.caret,
					this.selectionHighlight,
					this.string,
					this.number,
					this.operator,
					this.delimiter,
					this.type,
					this.identifier,
					this.comment,
					this.text,
					this.debugToken,
					this.debugTokenOutline,
					this.dockHighlight
				);
			}

			public Builder lineNumbersForeground(SerializableColor lineNumbersForeground) {
				this.lineNumbersForeground = lineNumbersForeground;
				return this;
			}

			public Builder lineNumbersBackground(SerializableColor lineNumbersBackground) {
				this.lineNumbersBackground = lineNumbersBackground;
				return this;
			}

			public Builder lineNumbersSelected(SerializableColor lineNumbersSelected) {
				this.lineNumbersSelected = lineNumbersSelected;
				return this;
			}

			public Builder obfuscated(SerializableColor obfuscated) {
				this.obfuscated = obfuscated;
				return this;
			}

			public Builder obfuscatedOutline(SerializableColor obfuscatedOutline) {
				this.obfuscatedOutline = obfuscatedOutline;
				return this;
			}

			public Builder proposed(SerializableColor proposed) {
				this.proposed = proposed;
				return this;
			}

			public Builder proposedOutline(SerializableColor proposedOutline) {
				this.proposedOutline = proposedOutline;
				return this;
			}

			public Builder deobfuscated(SerializableColor deobfuscated) {
				this.deobfuscated = deobfuscated;
				return this;
			}

			public Builder deobfuscatedOutline(SerializableColor deobfuscatedOutline) {
				this.deobfuscatedOutline = deobfuscatedOutline;
				return this;
			}

			public Builder editorBackground(SerializableColor editorBackground) {
				this.editorBackground = editorBackground;
				return this;
			}

			public Builder highlight(SerializableColor highlight) {
				this.highlight = highlight;
				return this;
			}

			public Builder caret(SerializableColor caret) {
				this.caret = caret;
				return this;
			}

			public Builder selectionHighlight(SerializableColor selectionHighlight) {
				this.selectionHighlight = selectionHighlight;
				return this;
			}

			public Builder string(SerializableColor string) {
				this.string = string;
				return this;
			}

			public Builder number(SerializableColor number) {
				this.number = number;
				return this;
			}

			public Builder operator(SerializableColor operator) {
				this.operator = operator;
				return this;
			}

			public Builder delimiter(SerializableColor delimiter) {
				this.delimiter = delimiter;
				return this;
			}

			public Builder type(SerializableColor type) {
				this.type = type;
				return this;
			}

			public Builder identifier(SerializableColor identifier) {
				this.identifier = identifier;
				return this;
			}

			public Builder comment(SerializableColor comment) {
				this.comment = comment;
				return this;
			}

			public Builder text(SerializableColor text) {
				this.text = text;
				return this;
			}

			public Builder debugToken(SerializableColor debugToken) {
				this.debugToken = debugToken;
				return this;
			}

			public Builder debugTokenOutline(SerializableColor debugTokenOutline) {
				this.debugTokenOutline = debugTokenOutline;
				return this;
			}

			public Builder dockHighlight(SerializableColor dockHighlight) {
				this.dockHighlight = dockHighlight;
				return this;
			}
		}
	}

	public static class LookAndFeelColors extends ReflectiveConfig.Section {
		public final TrackedValue<SerializableColor> foreground;
		public final TrackedValue<SerializableColor> background;

		public final TrackedValue<SerializableColor> accentBaseColor;

		public final TrackedValue<SerializableColor> activeCaption;
		public final TrackedValue<SerializableColor> inactiveCaption;

		public final TrackedValue<SerializableColor> errorBorder;

		public final TrackedValue<SerializableColor> warningBorder;

		private LookAndFeelColors(
				SerializableColor foreground,
				SerializableColor background,

				SerializableColor accentBaseColor,

				SerializableColor activeCaption,
				SerializableColor inactiveCaption,

				SerializableColor errorBorder,
				SerializableColor warningBorder
		) {
			this.foreground = this.value(foreground);
			this.background = this.value(background);

			this.accentBaseColor = this.value(accentBaseColor);

			this.activeCaption = this.value(activeCaption);
			this.inactiveCaption = this.value(inactiveCaption);

			this.errorBorder = this.value(errorBorder);
			this.warningBorder = this.value(warningBorder);
		}

		public void configure() {
			this.stream().forEach(Theme::resetIfAbsent);
		}

		public Stream<TrackedValue<SerializableColor>> stream() {
			return Stream.of(
				this.foreground,
				this.background,

				this.accentBaseColor,
				this.activeCaption,
				this.inactiveCaption,

				this.errorBorder,
				this.warningBorder
			);
		}

		public TrackedValue<SerializableColor> getWarningBorder() {
			return this.warningBorder;
		}

		public TrackedValue<SerializableColor> getErrorBorder() {
			return this.errorBorder;
		}

		public TrackedValue<SerializableColor> getInactiveCaption() {
			return this.inactiveCaption;
		}

		public TrackedValue<SerializableColor> getActiveCaption() {
			return this.activeCaption;
		}

		public TrackedValue<SerializableColor> getAccentBaseColor() {
			return this.accentBaseColor;
		}

		public TrackedValue<SerializableColor> getBackground() {
			return this.background;
		}

		public TrackedValue<SerializableColor> getForeground() {
			return this.foreground;
		}

		// default colors are from FlatLightLaf.properties
		public static class Builder {
			private SerializableColor foreground = new SerializableColor(0xFF000000);
			private SerializableColor background = new SerializableColor(0xFFF2F2F2);

			private SerializableColor accentBaseColor = new SerializableColor(0xFF2675BF);

			private SerializableColor activeCaption = new SerializableColor(0xFF99B4D1);
			private SerializableColor inactiveCaption = new SerializableColor(0xFFBFCDDB);

			private SerializableColor errorBorder = new SerializableColor(0xFFE53E4D);
			private SerializableColor warningBorder = new SerializableColor(0xFFE2A53A);

			public LookAndFeelColors build() {
				return new LookAndFeelColors(
					this.foreground,
					this.background,

					this.accentBaseColor,

					this.activeCaption,
					this.inactiveCaption,

					this.errorBorder,
					this.warningBorder
				);
			}

			public Builder foreground(SerializableColor foreground) {
				this.foreground = foreground;
				return this;
			}

			public Builder background(SerializableColor background) {
				this.background = background;
				return this;
			}

			public Builder accentBaseColor(SerializableColor accentBaseColor) {
				this.accentBaseColor = accentBaseColor;
				return this;
			}

			public Builder activeCaption(SerializableColor activeCaption) {
				this.activeCaption = activeCaption;
				return this;
			}

			public Builder inactiveCaption(SerializableColor inactiveCaption) {
				this.inactiveCaption = inactiveCaption;
				return this;
			}

			public Builder errorBorder(SerializableColor errorBorder) {
				this.errorBorder = errorBorder;
				return this;
			}

			public Builder warningBorder(SerializableColor warningBorder) {
				this.warningBorder = warningBorder;
				return this;
			}
		}
	}

	public static class SerializableColor extends Color implements ConfigSerializableObject<String> {
		SerializableColor(int rgba) {
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

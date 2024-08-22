package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.config.api.Config;
import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.values.ComplexConfigValue;
import org.quiltmc.config.api.values.ConfigSerializableObject;
import org.quiltmc.config.api.values.TrackedValue;

import java.awt.Color;
import java.util.function.Consumer;
import java.util.stream.Stream;

@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
public class ThemeCreator implements Config.Creator {
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

	public ThemeCreator(ThemeProperties properties) {
		this.properties = properties;
		this.syntaxPaneColors = properties.syntaxPaneColorsFactory.get().build();
		this.lookAndFeelColors = properties.lookAndFeelColorsFactory.get().build();
	}

	@Override
	public void create(Config.Builder builder) {
		builder.section("syntax_pane_colors", this.syntaxPaneColors);

		builder.section("look_and_feel_colors", this.lookAndFeelColors);
	}

	/**
	 * Default values are for light themes.
	 */
	public static class SyntaxPaneColors implements Consumer<Config.SectionBuilder> {
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
			this.lineNumbersForeground = TrackedValue.create(lineNumbersForeground, "lineNumbersForeground");
			this.lineNumbersBackground = TrackedValue.create(lineNumbersBackground, "lineNumbersBackground");
			this.lineNumbersSelected = TrackedValue.create(lineNumbersSelected, "lineNumbersSelected");
			this.obfuscated = TrackedValue.create(obfuscated, "obfuscated");
			this.obfuscatedOutline = TrackedValue.create(obfuscatedOutline, "obfuscatedOutline");

			this.proposed = TrackedValue.create(proposed, "proposed");
			this.proposedOutline = TrackedValue.create(proposedOutline, "proposedOutline");

			this.deobfuscated = TrackedValue.create(deobfuscated, "deobfuscated");
			this.deobfuscatedOutline = TrackedValue.create(deobfuscatedOutline, "deobfuscatedOutline");

			this.editorBackground = TrackedValue.create(editorBackground, "editorBackground");
			this.highlight = TrackedValue.create(highlight, "highlight");
			this.caret = TrackedValue.create(caret, "caret");
			this.selectionHighlight = TrackedValue.create(selectionHighlight, "selectionHighlight");
			this.string = TrackedValue.create(string, "string");
			this.number = TrackedValue.create(number, "number");
			this.operator = TrackedValue.create(operator, "operator");
			this.delimiter = TrackedValue.create(delimiter, "delimiter");
			this.type = TrackedValue.create(type, "type");
			this.identifier = TrackedValue.create(identifier, "identifier");
			this.comment = TrackedValue.create(comment, "comment");
			this.text = TrackedValue.create(text, "text");
			this.debugToken = TrackedValue.create(debugToken, "debugToken");
			this.debugTokenOutline = TrackedValue.create(debugTokenOutline, "debugTokenOutline");
			this.dockHighlight = TrackedValue.create(dockHighlight, "dockHighlight");
		}

		public void configure() {
			this.stream().forEach(ThemeCreator::resetIfAbsent);
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

		@Override
		public void accept(Config.SectionBuilder builder) {
			this.stream().forEach(builder::field);
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

	public static class LookAndFeelColors implements Consumer<Config.SectionBuilder> {
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
			this.foreground = TrackedValue.create(foreground, "foreground");
			this.background = TrackedValue.create(background, "background");

			this.accentBaseColor = TrackedValue.create(accentBaseColor, "accentBaseColor");

			this.activeCaption = TrackedValue.create(activeCaption, "activeCaption");
			this.inactiveCaption = TrackedValue.create(inactiveCaption, "inactiveCaption");

			this.errorBorder = TrackedValue.create(errorBorder, "errorBorder");
			this.warningBorder = TrackedValue.create(warningBorder, "warningBorder");
		}

		public void configure() {
			this.stream().forEach(ThemeCreator::resetIfAbsent);
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

		@Override
		public void accept(Config.SectionBuilder builder) {
			this.stream().forEach(builder::field);
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

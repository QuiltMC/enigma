package org.quiltmc.enigma.gui.config.theme.properties.composite;

import org.quiltmc.config.api.Config;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.config.theme.ThemeUtil;
import org.quiltmc.enigma.gui.config.theme.properties.ThemeProperties;

import java.util.function.Consumer;
import java.util.stream.Stream;

public class SyntaxPaneProperties implements Config.Creator, Configurable {
	public static final String COLORS_KEY = "syntax_pane_colors";

	public final Colors colors;

	public SyntaxPaneProperties() {
		this.colors = this.buildSyntaxPaneColors(new Colors.Builder()).build();
	}

	@Override
	public void create(Config.Builder builder) {
		builder.metadata(Comment.TYPE, ThemeProperties.SerializableColor::addFormatComment);
		builder.section(COLORS_KEY, this.colors);
	}

	protected Colors.Builder buildSyntaxPaneColors(Colors.Builder colors) {
		// start with default (light) colors
		return colors;
	}

	public void configure() {
		this.colors.configure();
	}

	/**
	 * Default values are for light themes.
	 */
	public static class Colors implements Consumer<Config.SectionBuilder> {
		public final TrackedValue<ThemeProperties.SerializableColor> lineNumbersForeground;
		public final TrackedValue<ThemeProperties.SerializableColor> lineNumbersBackground;
		public final TrackedValue<ThemeProperties.SerializableColor> lineNumbersSelected;
		public final TrackedValue<ThemeProperties.SerializableColor> obfuscated;
		public final TrackedValue<ThemeProperties.SerializableColor> obfuscatedOutline;

		public final TrackedValue<ThemeProperties.SerializableColor> proposed;
		public final TrackedValue<ThemeProperties.SerializableColor> proposedOutline;

		public final TrackedValue<ThemeProperties.SerializableColor> deobfuscated;
		public final TrackedValue<ThemeProperties.SerializableColor> deobfuscatedOutline;

		public final TrackedValue<ThemeProperties.SerializableColor> fallback;
		public final TrackedValue<ThemeProperties.SerializableColor> fallbackOutline;

		public final TrackedValue<ThemeProperties.SerializableColor> editorBackground;
		public final TrackedValue<ThemeProperties.SerializableColor> highlight;
		public final TrackedValue<ThemeProperties.SerializableColor> caret;
		public final TrackedValue<ThemeProperties.SerializableColor> selectionHighlight;
		public final TrackedValue<ThemeProperties.SerializableColor> string;
		public final TrackedValue<ThemeProperties.SerializableColor> number;
		public final TrackedValue<ThemeProperties.SerializableColor> operator;
		public final TrackedValue<ThemeProperties.SerializableColor> delimiter;
		public final TrackedValue<ThemeProperties.SerializableColor> type;
		public final TrackedValue<ThemeProperties.SerializableColor> identifier;
		public final TrackedValue<ThemeProperties.SerializableColor> comment;
		public final TrackedValue<ThemeProperties.SerializableColor> text;
		public final TrackedValue<ThemeProperties.SerializableColor> debugToken;
		public final TrackedValue<ThemeProperties.SerializableColor> debugTokenOutline;
		public final TrackedValue<ThemeProperties.SerializableColor> dockHighlight;

		private Colors(
				ThemeProperties.SerializableColor lineNumbersForeground,
				ThemeProperties.SerializableColor lineNumbersBackground,
				ThemeProperties.SerializableColor lineNumbersSelected,
				ThemeProperties.SerializableColor obfuscated,
				ThemeProperties.SerializableColor obfuscatedOutline,

				ThemeProperties.SerializableColor proposed,
				ThemeProperties.SerializableColor proposedOutline,

				ThemeProperties.SerializableColor deobfuscated,
				ThemeProperties.SerializableColor deobfuscatedOutline,

				ThemeProperties.SerializableColor fallback,
				ThemeProperties.SerializableColor fallbackOutline,

				ThemeProperties.SerializableColor editorBackground,
				ThemeProperties.SerializableColor highlight,
				ThemeProperties.SerializableColor caret,
				ThemeProperties.SerializableColor selectionHighlight,
				ThemeProperties.SerializableColor string,
				ThemeProperties.SerializableColor number,
				ThemeProperties.SerializableColor operator,
				ThemeProperties.SerializableColor delimiter,
				ThemeProperties.SerializableColor type,
				ThemeProperties.SerializableColor identifier,
				ThemeProperties.SerializableColor comment,
				ThemeProperties.SerializableColor text,
				ThemeProperties.SerializableColor debugToken,
				ThemeProperties.SerializableColor debugTokenOutline,
				ThemeProperties.SerializableColor dockHighlight
		) {
			this.lineNumbersForeground = TrackedValue.create(lineNumbersForeground, "line_numbers_foreground");
			this.lineNumbersBackground = TrackedValue.create(lineNumbersBackground, "line_numbers_background");
			this.lineNumbersSelected = TrackedValue.create(lineNumbersSelected, "line_numbers_selected");
			this.obfuscated = TrackedValue.create(obfuscated, "obfuscated");
			this.obfuscatedOutline = TrackedValue.create(obfuscatedOutline, "obfuscated_outline");

			this.proposed = TrackedValue.create(proposed, "proposed");
			this.proposedOutline = TrackedValue.create(proposedOutline, "proposed_outline");

			this.deobfuscated = TrackedValue.create(deobfuscated, "deobfuscated");
			this.deobfuscatedOutline = TrackedValue.create(deobfuscatedOutline, "deobfuscated_outline");

			this.fallback = TrackedValue.create(fallback, "fallback");
			this.fallbackOutline = TrackedValue.create(fallbackOutline, "fallbackOutline");

			this.editorBackground = TrackedValue.create(editorBackground, "editor_background");
			this.highlight = TrackedValue.create(highlight, "highlight");
			this.caret = TrackedValue.create(caret, "caret");
			this.selectionHighlight = TrackedValue.create(selectionHighlight, "selection_highlight");
			this.string = TrackedValue.create(string, "string");
			this.number = TrackedValue.create(number, "number");
			this.operator = TrackedValue.create(operator, "operator");
			this.delimiter = TrackedValue.create(delimiter, "delimiter");
			this.type = TrackedValue.create(type, "type");
			this.identifier = TrackedValue.create(identifier, "identifier");
			this.comment = TrackedValue.create(comment, "comment");
			this.text = TrackedValue.create(text, "text");
			this.debugToken = TrackedValue.create(debugToken, "debug_token");
			this.debugTokenOutline = TrackedValue.create(debugTokenOutline, "debug_token_outline");
			this.dockHighlight = TrackedValue.create(dockHighlight, "dock_highlight");
		}

		public void configure() {
			this.stream().forEach(ThemeUtil::resetIfAbsent);
		}

		public Stream<TrackedValue<ThemeProperties.SerializableColor>> stream() {
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

				this.fallback,
				this.fallbackOutline,

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
			private ThemeProperties.SerializableColor lineNumbersForeground = new ThemeProperties.SerializableColor(0xFF333300);
			private ThemeProperties.SerializableColor lineNumbersBackground = new ThemeProperties.SerializableColor(0xFFEEEEFF);
			private ThemeProperties.SerializableColor lineNumbersSelected = new ThemeProperties.SerializableColor(0xFFCCCCEE);
			private ThemeProperties.SerializableColor obfuscated = new ThemeProperties.SerializableColor(0xFFFFDCDC);
			private ThemeProperties.SerializableColor obfuscatedOutline = new ThemeProperties.SerializableColor(0xFFA05050);

			private ThemeProperties.SerializableColor proposed = new ThemeProperties.SerializableColor(0x27000000);
			private ThemeProperties.SerializableColor proposedOutline = new ThemeProperties.SerializableColor(0xBF000000);

			private ThemeProperties.SerializableColor deobfuscated = new ThemeProperties.SerializableColor(0xFFDCFFDC);
			private ThemeProperties.SerializableColor deobfuscatedOutline = new ThemeProperties.SerializableColor(0xFF50A050);

			private ThemeProperties.SerializableColor fallback = new ThemeProperties.SerializableColor(0xFFffddbb);
			private ThemeProperties.SerializableColor fallbackOutline = new ThemeProperties.SerializableColor(0xFFd86f06);

			private ThemeProperties.SerializableColor editorBackground = new ThemeProperties.SerializableColor(0xFFFFFFFF);
			private ThemeProperties.SerializableColor highlight = new ThemeProperties.SerializableColor(0xFF3333EE);
			private ThemeProperties.SerializableColor caret = new ThemeProperties.SerializableColor(0xFF000000);
			private ThemeProperties.SerializableColor selectionHighlight = new ThemeProperties.SerializableColor(0xFF000000);
			private ThemeProperties.SerializableColor string = new ThemeProperties.SerializableColor(0xFFCC6600);
			private ThemeProperties.SerializableColor number = new ThemeProperties.SerializableColor(0xFF999933);
			private ThemeProperties.SerializableColor operator = new ThemeProperties.SerializableColor(0xFF000000);
			private ThemeProperties.SerializableColor delimiter = new ThemeProperties.SerializableColor(0xFF000000);
			private ThemeProperties.SerializableColor type = new ThemeProperties.SerializableColor(0xFF000000);
			private ThemeProperties.SerializableColor identifier = new ThemeProperties.SerializableColor(0xFF000000);
			private ThemeProperties.SerializableColor comment = new ThemeProperties.SerializableColor(0xFF339933);
			private ThemeProperties.SerializableColor text = new ThemeProperties.SerializableColor(0xFF000000);
			private ThemeProperties.SerializableColor debugToken = new ThemeProperties.SerializableColor(0xFFD9BEF9);
			private ThemeProperties.SerializableColor debugTokenOutline = new ThemeProperties.SerializableColor(0xFFBD93F9);
			private ThemeProperties.SerializableColor dockHighlight = new ThemeProperties.SerializableColor(0xFF0000FF);

			public Colors build() {
				return new Colors(
					this.lineNumbersForeground,
					this.lineNumbersBackground,
					this.lineNumbersSelected,
					this.obfuscated,
					this.obfuscatedOutline,

					this.proposed,
					this.proposedOutline,

					this.deobfuscated,
					this.deobfuscatedOutline,

					this.fallback,
					this.fallbackOutline,

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

			public Builder lineNumbersForeground(ThemeProperties.SerializableColor lineNumbersForeground) {
				this.lineNumbersForeground = lineNumbersForeground;
				return this;
			}

			public Builder lineNumbersBackground(ThemeProperties.SerializableColor lineNumbersBackground) {
				this.lineNumbersBackground = lineNumbersBackground;
				return this;
			}

			public Builder lineNumbersSelected(ThemeProperties.SerializableColor lineNumbersSelected) {
				this.lineNumbersSelected = lineNumbersSelected;
				return this;
			}

			public Builder obfuscated(ThemeProperties.SerializableColor obfuscated) {
				this.obfuscated = obfuscated;
				return this;
			}

			public Builder obfuscatedOutline(ThemeProperties.SerializableColor obfuscatedOutline) {
				this.obfuscatedOutline = obfuscatedOutline;
				return this;
			}

			public Builder proposed(ThemeProperties.SerializableColor proposed) {
				this.proposed = proposed;
				return this;
			}

			public Builder proposedOutline(ThemeProperties.SerializableColor proposedOutline) {
				this.proposedOutline = proposedOutline;
				return this;
			}

			public Builder deobfuscated(ThemeProperties.SerializableColor deobfuscated) {
				this.deobfuscated = deobfuscated;
				return this;
			}

			public Builder deobfuscatedOutline(ThemeProperties.SerializableColor deobfuscatedOutline) {
				this.deobfuscatedOutline = deobfuscatedOutline;
				return this;
			}

			public Builder fallback(ThemeProperties.SerializableColor fallback) {
				this.fallback = fallback;
				return this;
			}

			public Builder fallbackOutline(ThemeProperties.SerializableColor fallbackOutline) {
				this.fallbackOutline = fallbackOutline;
				return this;
			}

			public Builder editorBackground(ThemeProperties.SerializableColor editorBackground) {
				this.editorBackground = editorBackground;
				return this;
			}

			public Builder highlight(ThemeProperties.SerializableColor highlight) {
				this.highlight = highlight;
				return this;
			}

			public Builder caret(ThemeProperties.SerializableColor caret) {
				this.caret = caret;
				return this;
			}

			public Builder selectionHighlight(ThemeProperties.SerializableColor selectionHighlight) {
				this.selectionHighlight = selectionHighlight;
				return this;
			}

			public Builder string(ThemeProperties.SerializableColor string) {
				this.string = string;
				return this;
			}

			public Builder number(ThemeProperties.SerializableColor number) {
				this.number = number;
				return this;
			}

			public Builder operator(ThemeProperties.SerializableColor operator) {
				this.operator = operator;
				return this;
			}

			public Builder delimiter(ThemeProperties.SerializableColor delimiter) {
				this.delimiter = delimiter;
				return this;
			}

			public Builder type(ThemeProperties.SerializableColor type) {
				this.type = type;
				return this;
			}

			public Builder identifier(ThemeProperties.SerializableColor identifier) {
				this.identifier = identifier;
				return this;
			}

			public Builder comment(ThemeProperties.SerializableColor comment) {
				this.comment = comment;
				return this;
			}

			public Builder text(ThemeProperties.SerializableColor text) {
				this.text = text;
				return this;
			}

			public Builder debugToken(ThemeProperties.SerializableColor debugToken) {
				this.debugToken = debugToken;
				return this;
			}

			public Builder debugTokenOutline(ThemeProperties.SerializableColor debugTokenOutline) {
				this.debugTokenOutline = debugTokenOutline;
				return this;
			}

			public Builder dockHighlight(ThemeProperties.SerializableColor dockHighlight) {
				this.dockHighlight = dockHighlight;
				return this;
			}
		}
	}
}

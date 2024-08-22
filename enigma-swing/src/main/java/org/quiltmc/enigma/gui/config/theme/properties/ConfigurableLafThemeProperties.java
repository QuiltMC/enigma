package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.config.api.Config;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.config.theme.look_and_feel.ConfigurableFlatLaf;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class ConfigurableLafThemeProperties extends NonSystemLafThemeProperties {
	private final LookAndFeelColors lookAndFeelColors;

	protected ConfigurableLafThemeProperties() {
		this.lookAndFeelColors = this.buildLookAndFeelColors(new LookAndFeelColors.Builder()).build();
	}

	@Override
	protected final LookAndFeel getLaf() {
		return this.getLafConstructor().construct(this.lookAndFeelColors);
	}

	@Override
	public void create(Config.Builder builder) {
		super.create(builder);

		builder.metadata(Comment.TYPE, ThemeProperties::addColorFormatComment);
		builder.section("look_and_feel_colors", this.buildLookAndFeelColors(new LookAndFeelColors.Builder()).build());
	}

	@Override
	public void configure() {
		super.configure();

		this.lookAndFeelColors.configure();
	}

	protected abstract ConfigurableFlatLaf.Constructor getLafConstructor();

	protected LookAndFeelColors.Builder buildLookAndFeelColors(LookAndFeelColors.Builder lookAndFeelColors) {
		// start with default (light) colors
		return lookAndFeelColors;
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
			this.stream().forEach(ThemeProperties::resetIfAbsent);
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
}

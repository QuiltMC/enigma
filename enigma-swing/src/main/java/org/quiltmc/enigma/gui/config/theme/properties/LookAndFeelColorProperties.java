package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.config.api.Config;
import org.quiltmc.config.api.annotations.Comment;

public class LookAndFeelColorProperties implements ConfigurableConfigCreator {
	public final ConfigurableLafThemeProperties.LookAndFeelColors lookAndFeelColors;

	protected LookAndFeelColorProperties() {
		this.lookAndFeelColors = this.buildLookAndFeelColors(
			new ConfigurableLafThemeProperties.LookAndFeelColors.Builder()
		).build();
	}

	@Override
	public void create(Config.Builder builder) {
		builder.metadata(Comment.TYPE, ThemeProperties::addColorFormatComment);
		builder.section("look_and_feel_colors", this.lookAndFeelColors);
	}

	@Override
	public void configure() {
		this.lookAndFeelColors.configure();
	}

	protected ConfigurableLafThemeProperties.LookAndFeelColors.Builder buildLookAndFeelColors(ConfigurableLafThemeProperties.LookAndFeelColors.Builder lookAndFeelColors) {
		// start with default (light) colors
		return lookAndFeelColors;
	}
}

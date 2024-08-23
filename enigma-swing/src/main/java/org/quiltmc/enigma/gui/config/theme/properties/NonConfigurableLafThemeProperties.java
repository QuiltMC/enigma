package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.config.api.Config;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.enigma.gui.config.theme.properties.composite.LookAndFeelProperties;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;
import org.quiltmc.enigma.gui.util.ListUtil;

import java.util.List;

public abstract class NonConfigurableLafThemeProperties extends NonSystemLafThemeProperties {
	public static void createComment(Config.Builder builder) {
		builder.metadata(Comment.TYPE, comment ->
			comment.add(LookAndFeelProperties.COLORS_KEY + " not configurable for this theme")
		);
	}

	protected NonConfigurableLafThemeProperties(
			SyntaxPaneProperties syntaxPaneColors,
			List<Config.Creator> creators
	) {
		super(syntaxPaneColors, ListUtil.prepend(NonConfigurableLafThemeProperties::createComment, creators));
	}
}

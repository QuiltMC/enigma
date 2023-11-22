package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.TrackedValue;

public class Theme extends ReflectiveConfig.Section {
	public final LookAndFeel lookAndFeel;
	public Theme(LookAndFeel lookAndFeel) {
		this.lookAndFeel = lookAndFeel;
	}

	public final TrackedValue<ThemeColors> colors = this.value(new ThemeColors());
	public final TrackedValue<ThemeFonts> fonts = this.value(new ThemeFonts());
}

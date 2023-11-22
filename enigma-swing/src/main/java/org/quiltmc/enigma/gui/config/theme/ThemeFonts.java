package org.quiltmc.enigma.gui.config.theme;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.util.ScaleUtil;

import java.awt.Font;

public class ThemeFonts extends ReflectiveConfig.Section {
	public final TrackedValue<Font> defaultFont = this.value(Font.decode(Font.DIALOG).deriveFont(Font.BOLD));
	public final TrackedValue<Font> small = this.value(ScaleUtil.scaleFont(Font.decode(Font.DIALOG)));
	public final TrackedValue<Font> editor = this.value(Font.decode(Font.MONOSPACED));
}

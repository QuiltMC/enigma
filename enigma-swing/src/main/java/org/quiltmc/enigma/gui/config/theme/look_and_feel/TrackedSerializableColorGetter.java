package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.config.theme.properties.ConfigurableLafThemeProperties;
import org.quiltmc.enigma.gui.config.theme.properties.ThemeProperties;

import java.util.function.Function;

@FunctionalInterface
public interface TrackedSerializableColorGetter extends Function
		<ConfigurableLafThemeProperties.LookAndFeelColors, TrackedValue<ThemeProperties.SerializableColor>> { }

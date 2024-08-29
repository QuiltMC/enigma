package org.quiltmc.enigma.gui.config.theme.look_and_feel;

import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.config.theme.properties.composite.LookAndFeelProperties;
import org.quiltmc.enigma.gui.config.theme.properties.ThemeProperties;

import java.util.function.Function;

@FunctionalInterface
public interface TrackedSerializableColorGetter extends Function
		<LookAndFeelProperties.Colors, TrackedValue<ThemeProperties.SerializableColor>> { }

package org.quiltmc.enigma.gui.config;

import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.gui.config.theme.ThemeChoice;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigTest {
	@Test
	void testAllThemeChoicesMapped() {
		assertTrue(
			Arrays.stream(ThemeChoice.values()).allMatch(choice -> Config.THEMES_BY_CHOICE.get(choice) != null)
		);
	}
}

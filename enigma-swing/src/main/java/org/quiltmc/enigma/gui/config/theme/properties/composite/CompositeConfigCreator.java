package org.quiltmc.enigma.gui.config.theme.properties.composite;

import org.quiltmc.config.api.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link Config.Creator} containing {@link ConfigurableConfigCreator}s, to work around
 * {@link org.quiltmc.config.api.ReflectiveConfig ReflectiveConfig} limitations.
 * <p>
 * {@link org.quiltmc.config.api.values.TrackedValue TrackedValue}s will have {@code null}
 * {@link Config}s if they're set by any class other than the one they're directly declared in.<br />
 * Having a separate
 * {@link Config.Creator} object that independently manages its own values works around this limitation.
 * <p>
 * Specifically, when properties where directly in
 * {@link org.quiltmc.enigma.gui.config.theme.properties.ThemeProperties ThemeProperties},<br />
 * {@link NullPointerException}s were thrown when calling {@code super.configure()} from subclasses;
 * {@code TrackedValueImpl.config} was {@code null}.
 */
public abstract class CompositeConfigCreator implements Config.Creator {
	private final List<ConfigurableConfigCreator> creators;

	public CompositeConfigCreator(Collection<ConfigurableConfigCreator> creators) {
		this.creators = new ArrayList<>(creators);
	}

	@Override
	public void create(Config.Builder builder) {
		creators.forEach(creator -> creator.create(builder));
	}

	public void configure() {
		creators.forEach(ConfigurableConfigCreator::configure);
	}
}

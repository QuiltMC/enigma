package org.quiltmc.enigma.gui.config.theme.properties;

import org.quiltmc.config.api.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

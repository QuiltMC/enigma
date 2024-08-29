package org.quiltmc.enigma.gui.config.theme.properties.composite;

import org.quiltmc.config.api.Config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * A {@link Config.Creator} containing other {@link Config.Creator}s, to work around
 * {@link org.quiltmc.config.api.ReflectiveConfig ReflectiveConfig} limitations.
 *
 * <p>
 * {@link org.quiltmc.config.api.values.TrackedValue TrackedValue}s will have {@code null}
 * {@link Config}s if they're set by any class other than the one they're directly declared in.<br />
 * Having a separate
 * {@link Config.Creator} object that independently manages its own values works around this limitation.
 *
 * <p>
 * Specifically, when properties where directly in
 * {@link org.quiltmc.enigma.gui.config.theme.properties.ThemeProperties ThemeProperties},<br />
 * {@link NullPointerException}s were thrown when calling {@code super.configure()} from subclasses;
 * {@code TrackedValueImpl.config} was {@code null}.
 */
public abstract class CompositeConfigCreator implements Config.Creator {
	private final List<Config.Creator> creators;
	private final List<Configurable> configurableCreators;

	public CompositeConfigCreator(Collection<Config.Creator> creators) {
		this.creators = List.copyOf(creators);

		this.configurableCreators = this.creators.stream()
				.flatMap(creator -> creator instanceof Configurable configurable
						? Stream.of(configurable)
						: Stream.empty()
				)
				.toList();
	}

	@Override
	public void create(Config.Builder builder) {
		this.creators.forEach(creator -> creator.create(builder));
	}

	public void configure() {
		this.configurableCreators.forEach(Configurable::configure);
	}
}

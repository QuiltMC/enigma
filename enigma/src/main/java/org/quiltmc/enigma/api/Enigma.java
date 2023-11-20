package org.quiltmc.enigma.api;

import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.mapping.MappingsIndex;
import org.quiltmc.enigma.api.service.EnigmaService;
import org.quiltmc.enigma.api.service.EnigmaServiceContext;
import org.quiltmc.enigma.api.service.EnigmaServiceFactory;
import org.quiltmc.enigma.api.service.EnigmaServiceType;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.class_provider.CachingClassProvider;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.class_provider.CombiningClassProvider;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.class_provider.ObfuscationFixClassProvider;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.util.Either;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Utils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

public class Enigma {
	public static final String NAME = "Enigma";
	public static final String VERSION;
	public static final String VINEFLOWER_VERSION;
	public static final String CFR_VERSION;
	public static final String PROCYON_VERSION;
	public static final String URL = "https://quiltmc.org";
	public static final int ASM_VERSION = Opcodes.ASM9;

	private final EnigmaProfile profile;
	private final EnigmaServices services;

	private Enigma(EnigmaProfile profile, EnigmaServices services) {
		this.profile = profile;
		this.services = services;
	}

	public static Enigma create() {
		return new Builder().build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public EnigmaProject openJar(Path path, ClassProvider libraryClassProvider, ProgressListener progress) throws IOException {
		JarClassProvider jarClassProvider = new JarClassProvider(path);
		JarIndex index = JarIndex.empty();
		ClassProvider classProvider = new ObfuscationFixClassProvider(new CachingClassProvider(new CombiningClassProvider(jarClassProvider, libraryClassProvider)), index);
		Set<String> scope = jarClassProvider.getClassNames();

		index.indexJar(scope, classProvider, progress);

		var indexers = this.services.get(JarIndexerService.TYPE);
		progress.init(indexers.size(), I18n.translate("progress.jar.custom_indexing"));

		int i = 1;
		for (var service : indexers) {
			progress.step(i++, I18n.translateFormatted("progress.jar.custom_indexing.indexer", service.getId()));
			service.acceptJar(scope, classProvider, index);
		}

		progress.step(i, I18n.translate("progress.jar.custom_indexing.finished"));

		var nameProposalServices = this.getNameProposalServices();
		progress.init(nameProposalServices.size(), I18n.translate("progress.jar.name_proposal"));

		EntryTree<EntryMapping> proposedNames = new HashEntryTree<>();

		int j = 1;
		for (var service : nameProposalServices) {
			progress.step(j++, I18n.translateFormatted("progress.jar.name_proposal.proposer", service.getId()));
			Map<Entry<?>, EntryMapping> proposed = service.getProposedNames(index);

			if (proposed != null) {
				for (var entry : proposed.entrySet()) {
					if (entry.getValue().tokenType() != TokenType.JAR_PROPOSED) {
						throw new RuntimeException("Token type of mapping " + entry.getValue() + " for entry " + entry.getKey() + " was " + entry.getValue().tokenType() + ", but should be " + TokenType.JAR_PROPOSED + "!");
					}

					proposedNames.insert(entry.getKey(), entry.getValue());
				}
			}
		}

		progress.step(j, I18n.translate("progress.jar.name_proposal.finished"));

		MappingsIndex mappingsIndex = MappingsIndex.empty();
		mappingsIndex.indexMappings(proposedNames, progress);

		return new EnigmaProject(this, path, classProvider, index, mappingsIndex, proposedNames, Utils.zipSha1(path));
	}

	public EnigmaProfile getProfile() {
		return this.profile;
	}

	public EnigmaServices getServices() {
		return this.services;
	}

	/**
	 * Gets all registered {@link NameProposalService name proposal services}, in the order that they should be run.
	 * This means that the first plugin declared in the profile will be run last -- that way, names it proposes take priority over those proposed by earlier-running plugins.
	 * @return the ordered list of services
	 */
	public List<NameProposalService> getNameProposalServices() {
		var proposalServices = new ArrayList<>(this.services.get(NameProposalService.TYPE));
		Collections.reverse(proposalServices);
		return proposalServices;
	}

	public static class Builder {
		private EnigmaProfile profile = EnigmaProfile.EMPTY;
		private Iterable<EnigmaPlugin> plugins = ServiceLoader.load(EnigmaPlugin.class);

		private Builder() {
		}

		public Builder setProfile(EnigmaProfile profile) {
			Preconditions.checkNotNull(profile, "profile cannot be null");
			this.profile = profile;
			return this;
		}

		public Builder setPlugins(Iterable<EnigmaPlugin> plugins) {
			Preconditions.checkNotNull(plugins, "plugins cannot be null");
			this.plugins = plugins;
			return this;
		}

		public Enigma build() {
			PluginContext pluginContext = new PluginContext(this.profile);
			for (EnigmaPlugin plugin : this.plugins) {
				plugin.init(pluginContext);
			}

			EnigmaServices services = pluginContext.buildServices();
			return new Enigma(this.profile, services);
		}
	}

	private static class PluginContext implements EnigmaPluginContext {
		private final EnigmaProfile profile;

		private final ImmutableListMultimap.Builder<EnigmaServiceType<?>, EnigmaService> services = ImmutableListMultimap.builder();

		PluginContext(EnigmaProfile profile) {
			this.profile = profile;
		}

		@Override
		public <T extends EnigmaService> void registerService(EnigmaServiceType<T> serviceType, EnigmaServiceFactory<T> factory) {
			List<EnigmaProfile.Service> serviceProfiles = this.profile.getServiceProfiles(serviceType);

			for (EnigmaProfile.Service serviceProfile : serviceProfiles) {
				T service = factory.create(this.getServiceContext(serviceProfile));
				if (serviceProfile.matches(service.getId())) {
					this.services.put(serviceType, service);
					break;
				}
			}
		}

		private <T extends EnigmaService> EnigmaServiceContext<T> getServiceContext(EnigmaProfile.Service serviceProfile) {
			return new EnigmaServiceContext<>() {
				@Override
				public Optional<Either<String, List<String>>> getArgument(String key) {
					return serviceProfile.getArgument(key);
				}

				@Override
				public Path getPath(String path) {
					return PluginContext.this.profile.resolvePath(Path.of(path));
				}
			};
		}

		/**
		 * Orders the services into the same order they were declared in the {@link EnigmaProfile profile}.
		 * @return the service container, with services ordered
		 */
		EnigmaServices buildServices() {
			var builtServices = this.services.build();
			ImmutableListMultimap.Builder<EnigmaServiceType<?>, EnigmaServices.RegisteredService<?>> orderedServices = ImmutableListMultimap.builder();
			for (EnigmaServiceType<?> type : builtServices.keySet()) {
				List<EnigmaProfile.Service> serviceProfiles = this.profile.getServiceProfiles(type);

				for (EnigmaProfile.Service service : serviceProfiles) {
					for (EnigmaServices.RegisteredService<?> registeredService : builtServices.get(type)) {
						if (service.matches(registeredService.id())) {
							orderedServices.put(type, registeredService);
							break;
						}
					}
				}
			}

			return new EnigmaServices(orderedServices.build());
		}
	}

	static {
		String version;
		String vf;
		String cfr;
		String procyon;

		try {
			Properties properties = Utils.readResourceToProperties("/version.properties");
			version = properties.getProperty("version");
			vf = properties.getProperty("vineflower-version");
			cfr = properties.getProperty("cfr-version");
			procyon = properties.getProperty("procyon-version");
		} catch (Exception ignored) {
			version = vf = cfr = procyon = "Unknown Version";
		}

		VERSION = version;
		VINEFLOWER_VERSION = vf;
		CFR_VERSION = cfr;
		PROCYON_VERSION = procyon;
	}
}

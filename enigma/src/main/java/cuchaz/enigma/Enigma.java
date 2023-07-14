package cuchaz.enigma;

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.api.EnigmaPlugin;
import cuchaz.enigma.api.EnigmaPluginContext;
import cuchaz.enigma.api.service.EnigmaService;
import cuchaz.enigma.api.service.EnigmaServiceContext;
import cuchaz.enigma.api.service.EnigmaServiceFactory;
import cuchaz.enigma.api.service.EnigmaServiceType;
import cuchaz.enigma.api.service.JarIndexerService;
import cuchaz.enigma.classprovider.CachingClassProvider;
import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.classprovider.CombiningClassProvider;
import cuchaz.enigma.classprovider.JarClassProvider;
import cuchaz.enigma.classprovider.ObfuscationFixClassProvider;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.Utils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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

		var indexers = this.services.getWithIds(JarIndexerService.TYPE);
		progress.init(indexers.size(), I18n.translate("progress.jar.custom_indexing"));

		int i = 1;
		for (var service : indexers) {
			progress.step(i++, I18n.translateFormatted("progress.jar.custom_indexing.indexer", service.id()));
			service.service().acceptJar(scope, classProvider, index);
		}

		progress.step(i, I18n.translate("progress.jar.custom_indexing.finished"));

		return new EnigmaProject(this, path, classProvider, index, Utils.zipSha1(path));
	}

	public EnigmaProfile getProfile() {
		return this.profile;
	}

	public EnigmaServices getServices() {
		return this.services;
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

		private final ImmutableListMultimap.Builder<EnigmaServiceType<?>, EnigmaServices.RegisteredService<?>> services = ImmutableListMultimap.builder();

		PluginContext(EnigmaProfile profile) {
			this.profile = profile;
		}

		@Override
		public <T extends EnigmaService> void registerService(String id, EnigmaServiceType<T> serviceType, EnigmaServiceFactory<T> factory) {
			List<EnigmaProfile.Service> serviceProfiles = this.profile.getServiceProfiles(serviceType);

			for (EnigmaProfile.Service serviceProfile : serviceProfiles) {
				if (serviceProfile.matches(id)) {
					T service = factory.create(this.getServiceContext(serviceProfile));
					this.services.put(serviceType, new EnigmaServices.RegisteredService<>(id, service));
					break;
				}
			}
		}

		private <T extends EnigmaService> EnigmaServiceContext<T> getServiceContext(EnigmaProfile.Service serviceProfile) {
			return new EnigmaServiceContext<>() {
				@Override
				public Optional<String> getArgument(String key) {
					return serviceProfile.getArgument(key);
				}

				@Override
				public Path getPath(String path) {
					return PluginContext.this.profile.resolvePath(Path.of(path));
				}
			};
		}

		EnigmaServices buildServices() {
			return new EnigmaServices(this.services.build());
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

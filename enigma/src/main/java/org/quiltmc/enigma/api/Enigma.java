package org.quiltmc.enigma.api;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.io.MoreFiles;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.quiltmc.enigma.api.analysis.index.jar.CombinedJarIndex;
import org.quiltmc.enigma.api.analysis.index.jar.LibrariesJarIndex;
import org.quiltmc.enigma.api.analysis.index.jar.MainJarIndex;
import org.quiltmc.enigma.api.analysis.index.mapping.MappingsIndex;
import org.quiltmc.enigma.api.class_provider.CachingClassProvider;
import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.class_provider.CombiningClassProvider;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.class_provider.ObfuscationFixClassProvider;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.impl.analysis.ClassLoaderClassProvider;
import org.quiltmc.enigma.api.service.EnigmaService;
import org.quiltmc.enigma.api.service.EnigmaServiceContext;
import org.quiltmc.enigma.api.service.EnigmaServiceFactory;
import org.quiltmc.enigma.api.service.EnigmaServiceType;
import org.quiltmc.enigma.api.service.JarIndexerService;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.service.ReadWriteService;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.serde.FileType;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.impl.analysis.index.AbstractJarIndex;
import org.quiltmc.enigma.util.Either;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Utils;
import org.quiltmc.enigma.util.Version;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class Enigma {
	public static final String NAME = "Enigma";
	public static final String VERSION;
	public static final String VINEFLOWER_VERSION;
	public static final String CFR_VERSION;
	public static final String PROCYON_VERSION;
	public static final String URL = "https://quiltmc.org";
	public static final int ASM_VERSION = Opcodes.ASM9;

	/**
	 * A compile-time constant which compilers can inline in
	 * {@link EnigmaPlugin#supportsEnigmaVersion(Version)} implementations,
	 * allowing plugins to 'remember' the version of Enigma they were built with.
	 */
	public static final int MAJOR_VERSION = 2;
	/**
	 * A compile-time constant which compilers can inline in
	 * {@link EnigmaPlugin#supportsEnigmaVersion(Version)} implementations,
	 * allowing plugins to 'remember' the version of Enigma they were built with.
	 */
	public static final int MINOR_VERSION = 7;
	/**
	 * A compile-time constant which compilers can inline in
	 * {@link EnigmaPlugin#supportsEnigmaVersion(Version)} implementations,
	 * allowing plugins to 'remember' the version of Enigma they were built with.
	 */
	public static final int PATCH_VERSION = 1;

	private static final Version CURRENT_VERSION = new Version(MAJOR_VERSION, MINOR_VERSION, PATCH_VERSION);

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
		MainJarIndex jarIndex = MainJarIndex.empty();
		LibrariesJarIndex libIndex = LibrariesJarIndex.empty();
		CombinedJarIndex comboIndex = CombinedJarIndex.empty(jarIndex, libIndex);

		ClassLoaderClassProvider jreProvider = new ClassLoaderClassProvider(DriverManager.class.getClassLoader());
		ClasspathClassProvider javaClassProvider = new ClasspathClassProvider();
		CombiningClassProvider librariesProvider = new CombiningClassProvider(jreProvider, javaClassProvider, libraryClassProvider);
		ClassProvider mainProjectProvider = new ObfuscationFixClassProvider(new CachingClassProvider(jarClassProvider), jarIndex);
		ProjectClassProvider projectClassProvider = new ProjectClassProvider(mainProjectProvider, librariesProvider);

		// main index
		this.index(jarIndex, projectClassProvider, progress, "jar", false);

		// lib index
		this.index(libIndex, projectClassProvider, progress, "libs", true);

		// combined main and lib index
		this.index(comboIndex, projectClassProvider, progress, "combined", true);

		// name proposal
		var nameProposalServices = this.getNameProposalServices();
		progress.init(nameProposalServices.size(), I18n.translate("progress.jar.name_proposal"));

		EntryTree<EntryMapping> proposedNames = new HashEntryTree<>();

		int j = 1;
		for (var service : nameProposalServices) {
			progress.step(j++, I18n.translateFormatted("progress.jar.name_proposal.proposer", service.getId()));
			Map<Entry<?>, EntryMapping> proposed = service.getProposedNames(this, jarIndex);

			if (proposed != null) {
				for (var entry : proposed.entrySet()) {
					service.validateProposedMapping(entry.getKey(), entry.getValue(), false);
					proposedNames.insert(entry.getKey(), entry.getValue());
				}
			}
		}

		progress.step(j, I18n.translate("progress.jar.name_proposal.finished"));

		MappingsIndex mappingsIndex = MappingsIndex.empty();
		mappingsIndex.indexMappings(proposedNames, progress);

		return new EnigmaProject(this, path, mainProjectProvider, jarIndex, libIndex, comboIndex, mappingsIndex, proposedNames, Utils.zipSha1(path));
	}

	private void index(
			AbstractJarIndex index, ProjectClassProvider classProvider, ProgressListener progress, String progressKey,
			boolean includesLibraries
	) {
		index.indexJar(classProvider, progress);

		List<JarIndexerService> indexers = this.services.get(JarIndexerService.TYPE);
		progress.init(indexers.size(), I18n.translate("progress." + progressKey + ".custom_indexing"));

		int i = 1;
		for (var service : indexers) {
			if (!(includesLibraries && !service.shouldIndexLibraries())) {
				progress.step(i++, I18n.translateFormatted("progress." + progressKey + ".custom_indexing.indexer", service.getId()));
				Set<String> scope = new HashSet<>(index.getIndexableClassNames(classProvider));
				service.acceptJar(scope, classProvider, index);
			}
		}

		progress.step(i, I18n.translate("progress." + progressKey + ".custom_indexing.finished"));
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

	/**
	 * {@return all registered read/write services}
	 */
	public List<ReadWriteService> getReadWriteServices() {
		return this.services.get(ReadWriteService.TYPE);
	}

	/**
	 * Gets all supported {@link FileType file types} for reading or writing.
	 * @return the list of supported file types
	 */
	public List<FileType> getSupportedFileTypes() {
		return this.getReadWriteServices().stream().map(ReadWriteService::getFileType).toList();
	}

	/**
	 * Gets the {@link ReadWriteService read/write service} for the provided {@link FileType file type}.
	 * @param fileType the file type to get the service for
	 * @return the read/write service for the file type
	 */
	public Optional<ReadWriteService> getReadWriteService(FileType fileType) {
		return this.getReadWriteServices().stream().filter(service -> service.getFileType().equals(fileType)).findFirst();
	}

	/**
	 * Parses the {@link FileType file type} of the provided path and returns the corresponding {@link ReadWriteService read/write service}
	 * @param path the path to analyse
	 * @return the read/write service for the file type of the path
	 */
	public Optional<ReadWriteService> getReadWriteService(Path path) {
		return this.parseFileType(path).flatMap(this::getReadWriteService);
	}

	/**
	 * Automatically determines the file type of and reads the provided mappings.
	 * @param path the path to read
	 * @return the read mappings
	 */
	public Optional<EntryTree<EntryMapping>> readMappings(Path path) throws MappingParseException, IOException {
		return this.readMappings(path, ProgressListener.createEmpty());
	}

	/**
	 * Automatically determines the file type of and reads the provided mappings.
	 * @param path the path to read
	 * @param progress a progress listener to use when reading
	 * @return the read mappings
	 */
	public Optional<EntryTree<EntryMapping>> readMappings(Path path, ProgressListener progress) throws MappingParseException, IOException {
		var service = this.getReadWriteService(path);

		if (service.isPresent()) {
			return Optional.ofNullable(service.get().read(path, progress));
		}

		return Optional.empty();
	}

	/**
	 * Searches for and returns a service with a matching id to the provided {@code id}.
	 * @param type the type of the searched service
	 * @param id the id of the service
	 * @return the optional service
	 */
	public <T extends EnigmaService> Optional<T> getService(EnigmaServiceType<T> type, String id) {
		for (T service : this.services.get(type)) {
			if (service.getId().equals(id)) {
				return Optional.of(service);
			}
		}

		return Optional.empty();
	}

	public static void validatePluginId(String id) {
		if (id != null && !id.matches("([a-z0-9_]+):([a-z0-9_]+((/[a-z0-9_]+)+)?)")) {
			throw new IllegalArgumentException("Invalid plugin id: \"" + id + "\"\n" + "Refer to Javadoc on EnigmaService#getId for how to properly form a service ID.");
		}
	}

	/**
	 * Determines the mapping format of the provided path. Checks all formats according to their {@link FileType} file extensions.
	 * If the path is a directory, it will check the first file in the directory. For directories, defaults to the enigma mappings format.
	 * @param path the path to analyse
	 * @return the mapping format of the path
	 */
	public Optional<FileType> parseFileType(Path path) {
		List<FileType> supportedTypes = this.getSupportedFileTypes();

		if (Files.isDirectory(path)) {
			try {
				final AtomicReference<Optional<File>> firstFile = new AtomicReference<>();
				firstFile.set(Optional.empty());

				Files.walkFileTree(path, new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						super.visitFile(file, attrs);
						if (attrs.isRegularFile()) {
							firstFile.set(Optional.of(file.toFile()));
							return FileVisitResult.TERMINATE;
						}

						return FileVisitResult.CONTINUE;
					}
				});

				if (firstFile.get().isPresent()) {
					for (FileType type : supportedTypes) {
						if (!type.isDirectory()) {
							continue;
						}

						String extension = MoreFiles.getFileExtension(firstFile.get().get().toPath()).toLowerCase();
						if (type.getExtensions().contains(extension)) {
							return Optional.of(type);
						}
					}
				}

				return this.getSupportedFileTypes().stream().filter(type -> type.isDirectory() && type.getExtensions().contains("mapping")).findFirst();
			} catch (Exception e) {
				Logger.error(e, "Failed to determine mapping format of directory {}", path);
			}
		} else {
			String extension = MoreFiles.getFileExtension(path).toLowerCase();

			for (FileType type : supportedTypes) {
				if (type.getExtensions().contains(extension)) {
					return Optional.of(type);
				}
			}
		}

		return Optional.empty();
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
				try {
					if (plugin.supportsEnigmaVersion(CURRENT_VERSION)) {
						plugin.init(pluginContext);
					} else {
						throw new IllegalStateException(
							"Plugin does not support Enigma %s: %s"
								.formatted(VERSION, plugin.getName())
						);
					}
				} catch (AbstractMethodError e) {
					throw new IllegalStateException(
						"""
						Error loading plugin: %s
						\tPlugin was probably built using a pre-2.7 version of Enigma.\
						""".formatted(plugin.getClass().getName()),
						e
					);
				}
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

			if (serviceProfiles.isEmpty() && serviceType.activeByDefault()) {
				this.putService(serviceType, factory.create(this.getServiceContext(null)));
				return;
			}

			for (EnigmaProfile.Service serviceProfile : serviceProfiles) {
				T service = factory.create(this.getServiceContext(serviceProfile));
				if (serviceProfile != null && serviceProfile.matches(service.getId())) {
					this.putService(serviceType, service);
					break;
				}
			}
		}

		private void putService(EnigmaServiceType<?> serviceType, EnigmaService service) {
			this.validateRegistration(this.services.build(), serviceType, service);
			this.services.put(serviceType, service);
		}

		private <T extends EnigmaService> EnigmaServiceContext<T> getServiceContext(EnigmaProfile.@Nullable Service serviceProfile) {
			return new EnigmaServiceContext<>() {
				@Override
				public Optional<Either<String, List<String>>> getArgument(String key) {
					return serviceProfile == null ? Optional.empty() : serviceProfile.getArgument(key);
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
			ImmutableListMultimap.Builder<EnigmaServiceType<?>, EnigmaService> orderedServices = ImmutableListMultimap.builder();
			for (EnigmaServiceType<?> type : builtServices.keySet()) {
				List<EnigmaProfile.Service> serviceProfiles = this.profile.getServiceProfiles(type);

				if (serviceProfiles.isEmpty() && type.activeByDefault()) {
					orderedServices.putAll(type, builtServices.get(type));
					continue;
				}

				for (EnigmaProfile.Service service : serviceProfiles) {
					for (EnigmaService registeredService : builtServices.get(type)) {
						if (service.matches(registeredService.getId())) {
							orderedServices.put(type, registeredService);
							break;
						}
					}
				}
			}

			var builtOrderedServices = orderedServices.build();
			return new EnigmaServices(builtOrderedServices);
		}

		private void validateRegistration(ImmutableListMultimap<EnigmaServiceType<?>, EnigmaService> services, EnigmaServiceType<?> serviceType, EnigmaService service) {
			validatePluginId(service.getId());

			for (EnigmaService otherService : services.get(serviceType)) {
				// all services
				if (service.getId().equals(otherService.getId())) {
					throw new IllegalStateException("Multiple services of type " + serviceType + " have the same ID: \"" + service.getId() + "\"");
				}

				// read write services
				if (service instanceof ReadWriteService rwService
						&& otherService instanceof ReadWriteService otherRwService
						&& rwService.getFileType().isDirectory() == otherRwService.getFileType().isDirectory()) {
					for (String extension : rwService.getFileType().getExtensions()) {
						if (otherRwService.getFileType().getExtensions().contains(extension)) {
							throw new IllegalStateException("Multiple read/write services found supporting the same extension: " + extension + " (id: " + service.getId() + ", other id: " + otherService.getId() + ")");
						}
					}
				}
			}
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

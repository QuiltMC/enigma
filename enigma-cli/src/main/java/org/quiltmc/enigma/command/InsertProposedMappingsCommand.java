package org.quiltmc.enigma.command;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.EnigmaPlugin;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingSaveParameters;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsWriter;
import org.quiltmc.enigma.api.translation.mapping.tree.DeltaTrackingTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.Utils;
import org.quiltmc.enigma.util.validation.ValidationContext;
import org.quiltmc.enigma.command.InsertProposedMappingsCommand.Required;
import org.quiltmc.enigma.command.InsertProposedMappingsCommand.Optional;
import org.tinylog.Logger;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

import static org.quiltmc.enigma.command.CommonArguments.DEOBFUSCATED_NAMESPACE;
import static org.quiltmc.enigma.command.CommonArguments.ENIGMA_PROFILE;
import static org.quiltmc.enigma.command.CommonArguments.INPUT_JAR;
import static org.quiltmc.enigma.command.CommonArguments.INPUT_MAPPINGS;
import static org.quiltmc.enigma.command.CommonArguments.MAPPING_OUTPUT;
import static org.quiltmc.enigma.command.CommonArguments.OBFUSCATED_NAMESPACE;

public final class InsertProposedMappingsCommand extends Command<Required, Optional> {
	public static final InsertProposedMappingsCommand INSTANCE = new InsertProposedMappingsCommand();

	private InsertProposedMappingsCommand() {
		super(
				ArgsParser.of(INPUT_JAR, INPUT_MAPPINGS, MAPPING_OUTPUT, Required::new),
				ArgsParser.of(ENIGMA_PROFILE, OBFUSCATED_NAMESPACE, DEOBFUSCATED_NAMESPACE, Optional::new)
		);
	}

	@Override
	void runImpl(Required required, Optional optional) throws Exception {
		run(
				required.inputJar, required.inputMappings, required.mappingOutput,
				optional.enigmaProfile, null,
				optional.obfuscatedNamespace, optional.deobfuscatedNamespace
		);
	}

	@Override
	public String getName() {
		return "insert-proposed-mappings";
	}

	@Override
	public String getDescription() {
		return "Adds all mappings proposed by the plugins on the classpath and declared in the profile into the given mappings.";
	}

	public static void run(Path inJar, Path source, Path output, @Nullable Path profilePath, @Nullable Iterable<EnigmaPlugin> plugins, @Nullable String obfuscatedNamespace, @Nullable String deobfuscatedNamespace) throws Exception {
		EnigmaProfile profile = EnigmaProfile.read(profilePath);
		Enigma enigma = createEnigma(profile, plugins);

		run(inJar, source, output, enigma, obfuscatedNamespace, deobfuscatedNamespace);
	}

	public static void run(Path inJar, Path source, Path output, Enigma enigma, @Nullable String obfuscatedNamespace, @Nullable String deobfuscatedNamespace) throws Exception {
		boolean debug = shouldDebug(INSTANCE.getName());
		int nameProposalServices = enigma.getServices().get(NameProposalService.TYPE).size();
		if (nameProposalServices == 0) {
			Logger.error("No name proposal services found!");
			return;
		}

		EnigmaProject project = openProject(inJar, source, enigma);
		DeltaTrackingTree<EntryMapping> mappings = project.getRemapper().getMappings();
		printStats(project);

		Utils.delete(output);
		MappingSaveParameters saveParameters = new MappingSaveParameters(enigma.getProfile().getMappingSaveParameters().fileNameFormat(), true, obfuscatedNamespace, deobfuscatedNamespace);

		MappingsWriter writer = CommandsUtil.getWriter(enigma, output);
		writer.write(mappings, output, ProgressListener.createEmpty(), saveParameters);

		if (debug) {
			writeDebugDelta(mappings, output);
		}
	}

	/**
	 * Adds all mappings proposed by the provided services, both bytecode-based and dynamic, to the project.
	 * @param nameProposalServices the name proposal services to run
	 * @param project the project
	 * @return the full mappings of the project
	 */
	@SuppressWarnings("unused")
	public static EntryTree<EntryMapping> exec(NameProposalService[] nameProposalServices, EnigmaProject project) {
		for (NameProposalService service : nameProposalServices) {
			Map<Entry<?>, EntryMapping> jarMappings = service.getProposedNames(project.getEnigma(), project.getJarIndex());
			Map<Entry<?>, EntryMapping> dynamicMappings = service.getDynamicProposedNames(project.getRemapper(), null, null, null);

			insertMappings(jarMappings, project);
			insertMappings(dynamicMappings, project);
		}

		return project.getRemapper().getMappings();
	}

	private static void insertMappings(@Nullable Map<Entry<?>, EntryMapping> mappings, EnigmaProject project) {
		if (mappings != null) {
			for (var entry : mappings.entrySet()) {
				if (entry.getValue() != null) {
					project.getRemapper().putMapping(new ValidationContext(null), entry.getKey(), entry.getValue());
				}
			}
		}
	}

	public static void printStats(EnigmaProject project) {
		EntryTree<EntryMapping> mappings = new HashEntryTree<>(project.getRemapper().getProposedMappings());
		AtomicInteger classes = new AtomicInteger();
		AtomicInteger fields = new AtomicInteger();
		AtomicInteger methods = new AtomicInteger();
		AtomicInteger parameters = new AtomicInteger();
		mappings.forEach(node -> {
			if (node.getValue() != null) {
				if (node.getEntry() instanceof ClassEntry) {
					classes.incrementAndGet();
				} else if (node.getEntry() instanceof FieldEntry) {
					fields.incrementAndGet();
				} else if (node.getEntry() instanceof MethodEntry) {
					methods.incrementAndGet();
				} else if (node.getEntry() instanceof LocalVariableEntry) {
					parameters.incrementAndGet();
				}
			}
		});

		Logger.info(
				"Proposed names for {} classes, {} fields, {} methods, {} parameters!",
				classes, fields, methods, parameters
		);
	}

	record Required(Path inputJar, Path inputMappings, Path mappingOutput) { }

	record Optional(Path enigmaProfile, String obfuscatedNamespace, String deobfuscatedNamespace) { }
}

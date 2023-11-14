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
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.Utils;
import org.tinylog.Logger;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

public class InsertProposedMappingsCommand extends Command {
	public InsertProposedMappingsCommand() {
		super(Argument.INPUT_JAR.required(),
				Argument.INPUT_MAPPINGS.required(),
				Argument.MAPPING_OUTPUT.required(),
				Argument.OUTPUT_MAPPING_FORMAT.required(),
				Argument.ENIGMA_PROFILE.optional());
	}

	@Override
	public void run(String... args) throws Exception {
		Path inJar = getReadablePath(this.getArg(args, 0));
		Path source = getReadablePath(this.getArg(args, 1));
		Path output = getWritablePath(this.getArg(args, 2));
		String resultFormat = this.getArg(args, 3);
		Path profilePath = getReadablePath(this.getArg(args, 4));

		run(inJar, source, output, resultFormat, profilePath, null);
	}

	@Override
	public String getName() {
		return "insert-proposed-mappings";
	}

	@Override
	public String getDescription() {
		return "Adds all mappings proposed by the plugins on the classpath and declared in the profile into the given mappings.";
	}

	public static void run(Path inJar, Path source, Path output, String resultFormat, @Nullable Path profilePath, @Nullable Iterable<EnigmaPlugin> plugins) throws Exception {
		EnigmaProfile profile = EnigmaProfile.read(profilePath);
		Enigma enigma = createEnigma(profile, plugins);

		run(inJar, source, output, resultFormat, enigma);
	}

	public static void run(Path inJar, Path source, Path output, String resultFormat, Enigma enigma) throws Exception {
		boolean debug = shouldDebug(new InsertProposedMappingsCommand().getName());
		int nameProposalServices = enigma.getServices().get(NameProposalService.TYPE).size();
		if (nameProposalServices == 0) {
			Logger.error("No name proposal services found!");
			return;
		}

		EnigmaProject project = openProject(inJar, source, enigma);
		DeltaTrackingTree<EntryMapping> mappings = project.getRemapper().getMappings();
		printStats(project);

		Utils.delete(output);
		MappingSaveParameters saveParameters = new MappingSaveParameters(enigma.getProfile().getMappingSaveParameters().fileNameFormat(), true);
		MappingsWriter writer = MappingCommandsUtil.getWriter(resultFormat);
		writer.write(mappings, output, ProgressListener.none(), saveParameters);

		if (debug) {
			writeDebugDelta(mappings, output);
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

		Logger.info("Proposed names for {} classes, {} fields, {} methods, {} parameters!", classes, fields, methods, parameters);
	}
}

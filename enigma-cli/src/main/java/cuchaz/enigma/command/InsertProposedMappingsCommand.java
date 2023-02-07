package cuchaz.enigma.command;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.api.EnigmaPlugin;
import cuchaz.enigma.api.service.NameProposalService;
import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.translation.ProposingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.DeltaTrackingTree;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.Utils;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.nio.file.Path;

public class InsertProposedMappingsCommand extends Command {
	private static final String NAME = "insert-proposed-mappings";

	public InsertProposedMappingsCommand() {
		super(NAME);
	}

	@Override
	public String getUsage() {
		return "<in jar> <source> <result> <result-format> [<profile>]";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length == 4 || length == 5;
	}

	@Override
	public void run(String... args) throws Exception {
		Path inJar = getReadablePath(getArg(args, 0, "in jar", true));
		Path source = getReadablePath(getArg(args, 1, "source", true));
		Path output = getWritablePath(getArg(args, 2, "result", true));
		String resultFormat = getArg(args, 3, "result-format", true);
		Path profilePath = getReadablePath(getArg(args, 4, "profile", false));

		run(inJar, source, output, resultFormat, profilePath, null);
	}

	public static void run(Path inJar, Path source, Path output, String resultFormat, @Nullable Path profilePath, @Nullable Iterable<EnigmaPlugin> plugins) throws Exception {
		boolean debug = shouldDebug(NAME);

		EnigmaProfile profile = EnigmaProfile.read(profilePath);
		Enigma.Builder builder = Enigma.builder().setProfile(profile);

		if (plugins != null) {
			builder.setPlugins(plugins);
		}

		Enigma enigma = builder.build();

		NameProposalService[] nameProposalServices = enigma.getServices().get(NameProposalService.TYPE).toArray(new NameProposalService[0]);
		if (nameProposalServices.length == 0) {
			Logger.error("No name proposal service found");
			return;
		}

		Logger.info("Reading JAR...");

		EnigmaProject project = enigma.openJar(inJar, new ClasspathClassProvider(), ProgressListener.none());

		Logger.info("Reading mappings...");

		MappingSaveParameters saveParameters = enigma.getProfile().getMappingSaveParameters();

		EntryTree<EntryMapping> mappings = readMappings(source, ProgressListener.none(), saveParameters);
		project.setMappings(mappings);

		if (debug) {
			mappings = new DeltaTrackingTree<>(mappings);
		}

		EntryRemapper mapper = project.getMapper();
		Translator translator = new ProposingTranslator(mapper, nameProposalServices);
		EntryIndex index = project.getJarIndex().getEntryIndex();

		Logger.info("Proposing class names...");
		int classes = 0;
		for (ClassEntry clazz : index.getClasses()) {
			if (insertMapping(clazz, mappings, mapper, translator)) {
				classes++;
			}
		}

		Logger.info("Proposing field names...");
		int fields = 0;
		for (FieldEntry field : index.getFields()) {
			if (insertMapping(field, mappings, mapper, translator)) {
				fields++;
			}
		}

		Logger.info("Proposing method and parameter names...");
		int methods = 0;
		int parameters = 0;
		for (MethodEntry method : index.getMethods()) {
			if (insertMapping(method, mappings, mapper, translator)) {
				methods++;
			}

			int p = index.getMethodAccess(method).isStatic() ? 0 : 1;
			for (TypeDescriptor paramDesc : method.getDesc().getArgumentDescs()) {
				LocalVariableEntry param = new LocalVariableEntry(method, p, "", true, null);
				if (insertMapping(param, mappings, mapper, translator)) {
					parameters++;
				}
				p += paramDesc.getSize();
			}
		}

		Logger.info("Proposed names for {} classes, {} fields, {} methods, {} parameters!", classes, fields, methods, parameters);

		Utils.delete(output);
		MappingCommandsUtil.write(mappings, resultFormat, output, saveParameters);

		if (debug) {
			writeDebugDelta((DeltaTrackingTree<EntryMapping>) mappings, output);
		}
	}

	private static <T extends Entry<?>> boolean insertMapping(T entry, EntryTree<EntryMapping> mappings, EntryRemapper mapper, Translator translator) {
		T deobf = mapper.extendedDeobfuscate(entry).getValue();
		String name = translator.extendedTranslate(entry).getValue().getName();
		if (!deobf.getName().equals(name) && !entry.getName().equals(name)) {
			String javadoc = deobf.getJavadocs();
			EntryMapping mapping = javadoc != null && !javadoc.isEmpty() ? new EntryMapping(name, javadoc) : new EntryMapping(name);
			mappings.insert(entry, mapping);
			return true;
		}

		return false;
	}
}

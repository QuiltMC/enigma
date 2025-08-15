package org.quiltmc.enigma.command;

import com.google.common.annotations.VisibleForTesting;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.EnigmaPlugin;
import org.quiltmc.enigma.api.analysis.index.jar.MainJarIndex;
import org.quiltmc.enigma.api.class_provider.CachingClassProvider;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.class_provider.JarClassProvider;
import org.quiltmc.enigma.api.class_provider.ProjectClassProvider;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.MappingDelta;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingsReader;
import org.quiltmc.enigma.api.translation.mapping.tree.DeltaTrackingTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.tinylog.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

public abstract sealed class Command<R, O> permits
		CheckMappingsCommand, ComposeMappingsCommand, ConvertMappingsCommand, DecompileCommand, DeobfuscateCommand,
		DropInvalidMappingsCommand, FillClassMappingsCommand, HelpCommand, InsertProposedMappingsCommand,
		InvertMappingsCommand, MapSpecializedMethodsCommand, PrintStatsCommand {
	private static final int EARLIEST_ARG_DELIM_INDEX = 1;

	@VisibleForTesting
	final ArgsParser<R> requiredArguments;
	@VisibleForTesting
	final ArgsParser<O> optionalArguments;

	private final int totalArgumentCount;

	Command(ArgsParser<R> requiredArguments, ArgsParser<O> optionalArguments) {
		this.requiredArguments = requiredArguments;
		this.optionalArguments = optionalArguments;
		this.totalArgumentCount = this.requiredArguments.count() + this.optionalArguments.count();
	}

	public String getUsage() {
		StringBuilder arguments = new StringBuilder();
		appendArguments(arguments, this.requiredArguments);

		if (!this.optionalArguments.isEmpty()) {
			arguments.append(" [");
			appendArguments(arguments, this.optionalArguments);
			arguments.append("]");
		}

		return arguments.toString();
	}

	private static void appendArguments(StringBuilder builder, ArgsParser<?> arguments) {
		final int argCount = arguments.count();
		final int iLastArg = argCount - 1;
		for (int i = 0; i < argCount; i++) {
			builder.append(arguments.get(i).getDisplayForm());
			if (i < iLastArg) {
				builder.append(" ");
			}
		}
	}

	private static void appendArgHelp(Argument<?> argument, int index, StringBuilder builder) {
		builder.append("Argument ").append(index).append(": ").append(argument.getDisplayForm()).append("\n");
		builder.append(argument.getExplanation()).append("\n");
	}

	final void appendHelp(StringBuilder builder) {
		builder.append("\t\t").append(this.getName()).append(" ").append(this.getUsage()).append("\n");

		int argIndex = 0;
		if (!this.requiredArguments.isEmpty()) {
			builder.append("Arguments:").append("\n");
			for (int j = 0; j < this.requiredArguments.count(); j++) {
				Argument<?> argument = this.requiredArguments.get(j);
				appendArgHelp(argument, argIndex, builder);
				argIndex++;
			}
		}

		if (!this.optionalArguments.isEmpty()) {
			builder.append("\n").append("Optional arguments:").append("\n");
			for (int i = 0; i < this.optionalArguments.count(); i++) {
				Argument<?> argument = this.optionalArguments.get(i);
				appendArgHelp(argument, argIndex, builder);
				argIndex++;
			}
		}
	}

	/**
	 * Ensures that the amount of arguments provided is valid to the command.
	 * @param length the amount of arguments passed in
	 * @return {@code true} if the argument count is valid, {@code false} otherwise
	 */
	public boolean checkArgumentCount(int length) {
		return length >= this.requiredArguments.count() && length <= this.totalArgumentCount;
	}

	/**
	 * Executes this command.
	 * @param args the command-line arguments
	 * @throws Exception on any error
	 */
	public final void run(String... args) throws Exception {
		if (args.length < this.requiredArguments.count()) {
			throw new ArgumentHelpException(
				this, "Too few arguments (%s); at least %s %s required.".formatted(
						args.length,
						this.requiredArguments.count() == 1 ? "is" : "are",
						this.requiredArguments.count()
					)
			);
		} else if (args.length > this.totalArgumentCount) {
			throw new ArgumentHelpException(
				this, "Too many arguments (%s); at most %s %s allowed.".formatted(
						args.length,
						this.totalArgumentCount == 1 ? "is" : "are",
						this.totalArgumentCount
					)
			);
		}

		final Map<String, String> valuesByName = new HashMap<>();
		final Set<String> positionalArgNames = new HashSet<>();

		final BiConsumer<String, Integer> addNamedArg = (rawValue, delim) -> {
			final String name = rawValue.substring(0, delim);
			if (positionalArgNames.contains(name)) {
				throw new ArgumentHelpException(
					this, "'%s' specified as both positional and named argument.".formatted(name)
				);
			}

			final String value = rawValue.substring(delim + 1);
			if (valuesByName.put(name, value) != null) {
				throw new ArgumentHelpException(this, "'%s' argument named more than once.".formatted(name));
			}
		};

		int i = 0;
		// leading positional args
		for (; i < args.length; i++) {
			final String rawValue = args[i];
			final int delim = indexOfNameDelim(rawValue);
			if (delim < EARLIEST_ARG_DELIM_INDEX) {
				final Argument<?> arg = i < this.requiredArguments.count()
						? this.requiredArguments.get(i)
						: this.optionalArguments.get(i - this.requiredArguments.count());

				valuesByName.put(arg.getName(), rawValue);
				positionalArgNames.add(arg.getName());
			} else {
				// first named arg
				addNamedArg.accept(rawValue, delim);
				i++;
				break;
			}
		}

		// trailing named args
		for (; i < args.length; i++) {
			final String rawValue = args[i];
			final int delim = indexOfNameDelim(rawValue);
			if (delim < EARLIEST_ARG_DELIM_INDEX) {
				throw new ArgumentHelpException(
					this, "Found unnamed positional argument after named a argument; "
							+ "all positional arguments must come before any named arguments. Unnamed argument:\n\t"
							+ rawValue
				);
			} else {
				addNamedArg.accept(rawValue, delim);
			}
		}

		this.runImpl(
				this.requiredArguments.parse(valuesByName, Argument::requireFrom),
				this.optionalArguments.parse(valuesByName, Argument::from)
		);
	}

	/**
	 * Executes this command.
	 * @param required packed non-null required argument values
	 * @param optional packed optional argument values
	 */
	abstract void runImpl(R required, O optional) throws Exception;

	/**
	 * Returns the name of this command. Should be all-lowercase, and separated by dashes for words.
	 * Examples: {@code decompile}, {@code compose-mappings}, {@code fill-class-mappings}
	 * @return the name of the command
	 */
	public abstract String getName();

	/**
	 * Returns a one-sentence description of this command's function, used in {@link HelpCommand}.
	 * @return the description
	 */
	public abstract String getDescription();

	public static JarIndex loadJar(Path jar) throws IOException {
		Logger.info("Reading JAR...");
		JarClassProvider classProvider = new JarClassProvider(jar);
		JarIndex index = MainJarIndex.empty();
		index.indexJar(new ProjectClassProvider(new CachingClassProvider(classProvider), null), ProgressListener.createEmpty());

		return index;
	}

	public static Enigma createEnigma() {
		return Enigma.create();
	}

	public static Enigma createEnigma(EnigmaProfile profile, @Nullable Iterable<EnigmaPlugin> plugins) {
		Enigma.Builder builder = Enigma.builder().setProfile(profile);

		if (plugins != null) {
			builder.setPlugins(plugins);
		}

		return builder.build();
	}

	protected static EnigmaProject openProject(Path fileJarIn, Path fileMappings) throws Exception {
		return openProject(fileJarIn, fileMappings, createEnigma());
	}

	public static EnigmaProject openProject(Path fileJarIn, Path fileMappings, Enigma enigma) throws Exception {
		ProgressListener progress = new ConsoleProgressListener();

		Logger.info("Reading JAR...");
		EnigmaProject project = enigma.openJar(fileJarIn, new ClasspathClassProvider(), progress);

		if (fileMappings != null) {
			Logger.info("Reading mappings...");

			EntryTree<EntryMapping> mappings = readMappings(enigma, fileMappings, progress);

			project.setMappings(mappings, new ConsoleProgressListener());
		}

		return project;
	}

	protected static EntryTree<EntryMapping> readMappings(Enigma enigma, Path path, ProgressListener progress) throws MappingParseException, IOException {
		MappingsReader reader = CommandsUtil.getReader(enigma, path);
		return reader.read(path, progress);
	}

	private static String getArg(String[] args, int index, Argument<?> argument, boolean optional) {
		if (index >= args.length) {
			if (!optional) {
				throw new IllegalArgumentException(argument.getName() + " is required");
			} else {
				return null;
			}
		}

		return args[index];
	}

	private static int indexOfNameDelim(String argValue) {
		for (int ic = 0; ic < argValue.length(); ic++) {
			final char c = argValue.charAt(ic);
			if (c == Argument.SEPARATOR) {
				return -1;
			} else if (c == Argument.NAME_DELIM) {
				return ic;
			}
		}

		return -1;
	}

	protected static boolean shouldDebug(String name) {
		String key = String.format("enigma.%s.debug", name);
		return System.getProperty(key, "false").toLowerCase(Locale.ROOT).equals("true");
	}

	protected static <T> void writeDebugDelta(DeltaTrackingTree<T> tree, Path output) throws IOException {
		Path debugOutput = output.resolveSibling("debug-" + output.getFileName() + ".txt");
		MappingDelta<T> delta = tree.takeDelta();

		try (BufferedWriter writer = Files.newBufferedWriter(debugOutput)) {
			List<String> content = delta.getChanges().getAllEntries().map(Object::toString).toList();
			for (String s : content) {
				writer.write(s);
				writer.newLine();
			}
		}

		Logger.debug("Wrote debug output to {}", debugOutput.toAbsolutePath());
	}

	public static class ConsoleProgressListener extends ProgressListener {
		private static final int REPORT_TIME = 5000; // 5s

		private long startTime;
		private long lastReportTime;

		@Override
		public void init(int totalWork, String title) {
			super.init(totalWork, title);
			this.startTime = System.currentTimeMillis();
			this.lastReportTime = this.startTime;
			Logger.info(title);
		}

		@Override
		public void step(int workDone, String message) {
			super.step(workDone, message);
			long now = System.currentTimeMillis();
			boolean isLastUpdate = workDone == this.totalWork;
			boolean shouldReport = isLastUpdate || now - this.lastReportTime > REPORT_TIME;

			if (shouldReport) {
				int percent = workDone * 100 / this.totalWork;
				Logger.info("\tProgress: {}%", percent);
				this.lastReportTime = now;
			}

			if (isLastUpdate) {
				double elapsedSeconds = (now - this.startTime) / 1000.0;
				Logger.info("Finished in {} seconds", elapsedSeconds);
			}
		}
	}

	abstract static class HelpException extends RuntimeException {
		protected abstract Command getCommand();

		HelpException(String message) {
			super(message);
		}

		HelpException(Throwable cause) {
			super(cause);
		}
	}

	protected static class ArgumentHelpException extends HelpException {
		private final Command<?, ?> command;

		protected ArgumentHelpException(Command<?, ?> command, String message) {
			super(message);
			this.command = command;
		}

		@Override
		public Command<?, ?> getCommand() {
			return this.command;
		}
	}
}

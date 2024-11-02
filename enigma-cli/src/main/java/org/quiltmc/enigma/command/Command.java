package org.quiltmc.enigma.command;

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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

public abstract class Command {
	protected final List<Argument> requiredArguments = new ArrayList<>();
	protected final List<Argument> optionalArguments = new ArrayList<>();
	protected final List<ComposedArgument> allArguments;

	protected Command(ComposedArgument... arguments) {
		this.allArguments = new ArrayList<>();

		for (ComposedArgument argument : arguments) {
			if (argument.optional()) {
				this.optionalArguments.add(argument.argument());
				this.allArguments.add(argument);
			} else {
				this.requiredArguments.add(argument.argument());
				this.allArguments.add(argument);

				if (!this.optionalArguments.isEmpty()) {
					throw new IllegalArgumentException("optional arguments should be grouped at the end of command arguments! (declaring arg " + argument + ")");
				}
			}
		}
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

	private static void appendArguments(StringBuilder builder, List<Argument> arguments) {
		for (int i = 0; i < arguments.size(); i++) {
			builder.append(arguments.get(i).getDisplayForm());
			if (i < arguments.size() - 1) {
				builder.append(" ");
			}
		}
	}

	/**
	 * Ensures that the amount of arguments provided is valid to the command.
	 * @param length the amount of arguments passed in
	 * @return {@code true} if the argument count is valid, {@code false} otherwise
	 */
	public boolean checkArgumentCount(int length) {
		// valid if length is equal to the amount of required arguments or between required argument count and total argument count
		return length == this.requiredArguments.size() || length > this.requiredArguments.size() && length <= this.allArguments.size();
	}

	/**
	 * Executes this command.
	 * @param args the command-line arguments, to be parsed with {@link #getArg(String[], int)}
	 * @throws Exception on any error
	 */
	public abstract void run(String... args) throws Exception;

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

	/**
	 * Parses and validates the argument at {@code index}. The argument can then be converted to something more useful via {@link #getReadablePath(String)}, {@link #getWritablePath(String)}, etc.
	 * @param args the command-line args, provided in {@link #run(String...)}
	 * @param index the index of the argument
	 * @return the argument, as a string
	 */
	protected String getArg(String[] args, int index) {
		if (index < this.allArguments.size() && index >= 0) {
			return getArg(args, index, this.allArguments.get(index));
		} else {
			throw new RuntimeException("arg index is outside of range of possible arguments! (index: " + index + ", allowed arg count: " + this.allArguments.size() + ")");
		}
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

	protected static File getWritableFile(String path) {
		if (path == null) {
			return null;
		}

		File file = new File(path).getAbsoluteFile();
		File dir = file.getParentFile();
		if (dir == null) {
			throw new IllegalArgumentException("Cannot write file: " + path);
		}

		// quick fix to avoid stupid stuff in Gradle code
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}

		return file;
	}

	protected static File getWritableFolder(String path) {
		if (path == null) {
			return null;
		}

		File dir = new File(path).getAbsoluteFile();
		if (!dir.exists()) {
			throw new IllegalArgumentException("Cannot write to folder: " + dir);
		}

		return dir;
	}

	protected static File getReadableFile(String path) {
		if (path == null) {
			return null;
		}

		File file = new File(path).getAbsoluteFile();
		if (!file.exists()) {
			throw new IllegalArgumentException("Cannot find file: " + file.getAbsolutePath());
		}

		return file;
	}

	protected static Path getReadablePath(String path) {
		if (path == null) {
			return null;
		}

		Path file = Paths.get(path).toAbsolutePath();
		if (!Files.exists(file)) {
			throw new IllegalArgumentException("Cannot find file: " + file);
		}

		return file;
	}

	protected static Path getWritablePath(String path) {
		if (path == null) {
			return null;
		}

		Path dir = Path.of(path).toAbsolutePath();

		try {
			if (!Files.exists(dir.getParent())) {
				Files.createDirectories(dir.getParent());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return dir;
	}

	private static String getArg(String[] args, int i, ComposedArgument argument) {
		if (i >= args.length) {
			if (!argument.optional()) {
				throw new IllegalArgumentException(argument.argument().getDisplayForm() + " is required");
			} else {
				return null;
			}
		}

		return args[i];
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
}

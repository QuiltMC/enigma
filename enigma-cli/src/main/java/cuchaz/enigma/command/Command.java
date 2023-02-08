package cuchaz.enigma.command;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.tree.DeltaTrackingTree;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.common.io.MoreFiles;
import org.tinylog.Logger;

public abstract class Command {
	public final String name;

	protected Command(String name) {
		this.name = name;
	}

	public abstract String getUsage();

	public abstract boolean isValidArgument(int length);

	public abstract void run(String... args) throws Exception;

	protected static EnigmaProject openProject(Path fileJarIn, Path fileMappings) throws Exception {
		ProgressListener progress = new ConsoleProgressListener();

		Enigma enigma = Enigma.create();

		Logger.info("Reading jar...");
		EnigmaProject project = enigma.openJar(fileJarIn, new ClasspathClassProvider(), progress);

		if (fileMappings != null) {
			Logger.info("Reading mappings...");

			MappingSaveParameters saveParameters = enigma.getProfile().getMappingSaveParameters();
			EntryTree<EntryMapping> mappings = readMappings(fileMappings, progress, saveParameters);

			project.setMappings(mappings);
		}

		return project;
	}

	protected static EntryTree<EntryMapping> readMappings(Path path, ProgressListener progress, MappingSaveParameters saveParameters) throws Exception {
		List<Exception> suppressed = new ArrayList<>();
		if ("zip".equalsIgnoreCase(MoreFiles.getFileExtension(path))) {
			return MappingFormat.ENIGMA_ZIP.read(path, progress, saveParameters);
		} else {
			for (MappingFormat format : MappingFormat.getReadableFormats()) {
				try {
					return format.read(path, progress, saveParameters);
				} catch (Exception e) {
					suppressed.add(e);
				}
			}
		}
		RuntimeException exception = new RuntimeException("Unable to parse mappings!");
		for (Exception supressedException : suppressed) {
			exception.addSuppressed(supressedException);
		}
		throw exception;
	}

	protected static void writeMappings(EntryTree<EntryMapping> mappings, Path path, ProgressListener progress, MappingSaveParameters saveParameters) throws Exception {
		List<Exception> suppressed = new ArrayList<>();
		if ("zip".equalsIgnoreCase(MoreFiles.getFileExtension(path))) {
			MappingFormat.ENIGMA_ZIP.write(mappings, path, progress, saveParameters);
		} else {
			for (MappingFormat format : MappingFormat.getWritableFormats()) {
				try {
					format.write(mappings, path, progress, saveParameters);
					return;
				} catch (Exception e) {
					suppressed.add(e);
				}
			}
		}

		RuntimeException exception = new RuntimeException("Unable to write mappings!");
		for (Exception supressedException : suppressed) {
			exception.addSuppressed(supressedException);
		}
		throw exception;
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

	protected static String getArg(String[] args, int i, String name, boolean required) {
		if (i >= args.length) {
			if (required) {
				throw new IllegalArgumentException(name + " is required");
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

	public static class ConsoleProgressListener implements ProgressListener {

		private static final int ReportTime = 5000; // 5s

		private int totalWork;
		private long startTime;
		private long lastReportTime;

		@Override
		public void init(int totalWork, String title) {
			this.totalWork = totalWork;
			this.startTime = System.currentTimeMillis();
			this.lastReportTime = this.startTime;
			Logger.info(title);
		}

		@Override
		public void step(int numDone, String message) {
			long now = System.currentTimeMillis();
			boolean isLastUpdate = numDone == this.totalWork;
			boolean shouldReport = isLastUpdate || now - this.lastReportTime > ReportTime;

			if (shouldReport) {
				int percent = numDone * 100 / this.totalWork;
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

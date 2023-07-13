package cuchaz.enigma.network;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.classprovider.ClasspathClassProvider;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.utils.Utils;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class DedicatedEnigmaServer extends EnigmaServer {
	private final EnigmaProfile profile;
	private final MappingFormat mappingFormat;
	private final Path mappingsFile;
	private final PrintWriter log;
	private final BlockingQueue<Runnable> tasks = new LinkedBlockingDeque<>();

	public DedicatedEnigmaServer(
			byte[] jarChecksum,
			char[] password,
			EnigmaProfile profile,
			MappingFormat mappingFormat,
			Path mappingsFile,
			PrintWriter log,
			EntryRemapper mappings,
			int port
	) {
		super(jarChecksum, password, mappings, port);
		this.profile = profile;
		this.mappingFormat = mappingFormat;
		this.mappingsFile = mappingsFile;
		this.log = log;
	}

	@Override
	protected void runOnThread(Runnable task) {
		this.tasks.add(task);
	}

	@Override
	public void log(String message) {
		super.log(message);
		this.log.println(message);
	}

	public static void main(String[] args) {
		OptionParser parser = new OptionParser();

		OptionSpec<Path> jarOpt = parser.accepts("jar", "Jar file to open at startup")
				.withRequiredArg()
				.required()
				.withValuesConvertedBy(PathConverter.INSTANCE);

		OptionSpec<Path> mappingsOpt = parser.accepts("mappings", "Mappings file to open at startup")
				.withRequiredArg()
				.required()
				.withValuesConvertedBy(PathConverter.INSTANCE);

		OptionSpec<Path> profileOpt = parser.accepts("profile", "Profile json to apply at startup")
				.withRequiredArg()
				.withValuesConvertedBy(PathConverter.INSTANCE);

		OptionSpec<Integer> portOpt = parser.accepts("port", "Port to run the server on")
				.withOptionalArg()
				.ofType(Integer.class)
				.defaultsTo(EnigmaServer.DEFAULT_PORT);

		OptionSpec<String> passwordOpt = parser.accepts("password", "The password to join the server")
				.withRequiredArg()
				.defaultsTo("");

		OptionSpec<Path> logFileOpt = parser.accepts("log", "The log file to write to")
				.withRequiredArg()
				.withValuesConvertedBy(PathConverter.INSTANCE)
				.defaultsTo(Paths.get("log.txt"));

		OptionSet parsedArgs = parser.parse(args);
		Path jar = parsedArgs.valueOf(jarOpt);
		Path mappingsFile = parsedArgs.valueOf(mappingsOpt);
		Path profileFile = parsedArgs.valueOf(profileOpt);
		int port = parsedArgs.valueOf(portOpt);
		char[] password = parsedArgs.valueOf(passwordOpt).toCharArray();
		if (password.length > EnigmaServer.MAX_PASSWORD_LENGTH) {
			Logger.error("Password too long, must be at most {} characters", EnigmaServer.MAX_PASSWORD_LENGTH);
			System.exit(1);
		}

		Path logFile = parsedArgs.valueOf(logFileOpt);

		Logger.info("Starting Enigma server");
		DedicatedEnigmaServer server;
		try {
			byte[] checksum = Utils.zipSha1(parsedArgs.valueOf(jarOpt));

			EnigmaProfile profile = EnigmaProfile.read(profileFile);
			Enigma enigma = Enigma.builder().setProfile(profile).build();
			Logger.info("Indexing Jar...");
			EnigmaProject project = enigma.openJar(jar, new ClasspathClassProvider(), ProgressListener.none());

			MappingFormat mappingFormat = MappingFormat.parseFromFile(mappingsFile);
			EntryRemapper mappings;
			if (!Files.exists(mappingsFile)) {
				mappings = EntryRemapper.empty(project.getJarIndex());
			} else {
				Logger.info("Reading mappings...");
				mappings = EntryRemapper.mapped(project.getJarIndex(), mappingFormat.read(mappingsFile, ProgressListener.none(), profile.getMappingSaveParameters()));
			}

			PrintWriter log = new PrintWriter(Files.newBufferedWriter(logFile));

			server = new DedicatedEnigmaServer(checksum, password, profile, mappingFormat, mappingsFile, log, mappings, port);
			server.start();
			Logger.info("Server started");
		} catch (IOException | MappingParseException e) {
			Logger.error(e, "Error starting server!");
			System.exit(1);
			return;
		}

		// noinspection RedundantSuppression
		// noinspection Convert2MethodRef - javac 8 bug
		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> server.runOnThread(() -> server.saveMappings()), 0, 1, TimeUnit.MINUTES);
		Runtime.getRuntime().addShutdownHook(new Thread(server::saveMappings));

		while (true) {
			try {
				server.tasks.take().run();
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	@Override
	public synchronized void stop() {
		super.stop();
		System.exit(0);
	}

	private void saveMappings() {
		this.mappingFormat.write(this.getMappings().getObfToDeobf(), this.getMappings().takeMappingDelta(), this.mappingsFile, ProgressListener.none(), this.profile.getMappingSaveParameters());
		this.log.flush();
	}

	public static class PathConverter implements ValueConverter<Path> {
		public static final ValueConverter<Path> INSTANCE = new PathConverter();

		PathConverter() {
		}

		@Override
		public Path convert(String path) {
			// expand ~ to the home dir
			if (path.startsWith("~")) {
				// get the home dir
				Path dirHome = Paths.get(System.getProperty("user.home"));

				// is the path just ~/ or is it ~user/ ?
				if (path.startsWith("~/")) {
					return dirHome.resolve(path.substring(2));
				} else {
					return dirHome.getParent().resolve(path.substring(1));
				}
			}

			return Paths.get(path);
		}

		@Override
		public Class<? extends Path> valueType() {
			return Path.class;
		}

		@Override
		public String valuePattern() {
			return "path";
		}
	}
}

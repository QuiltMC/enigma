package org.quiltmc.enigma.gui;

import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.config.theme.ThemeUtil;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.dialog.CrashDialog;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.validation.Message;
import org.quiltmc.enigma.util.validation.ParameterizedMessage;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;
import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class Main {
	public static void main(String[] args) throws IOException {
		OptionParser parser = new OptionParser();

		OptionSpec<Path> jar = parser.accepts("jar", "Jar file to open at startup")
				.withRequiredArg()
				.withValuesConvertedBy(PathConverter.INSTANCE);

		OptionSpec<Path> mappings = parser.accepts("mappings", "Mappings file to open at startup")
				.withRequiredArg()
				.withValuesConvertedBy(PathConverter.INSTANCE);

		OptionSpec<Path> profile = parser.accepts("profile", "Profile json to apply at startup")
				.withRequiredArg()
				.withValuesConvertedBy(PathConverter.INSTANCE);

		parser.acceptsAll(List.of("edit-all", "e"), "Enable editing everything");
		parser.acceptsAll(List.of("no-edit-all", "E"), "Disable editing everything");
		parser.acceptsAll(List.of("edit-classes", "c"), "Enable editing class names");
		parser.acceptsAll(List.of("no-edit-classes", "C"), "Disable editing class names");
		parser.acceptsAll(List.of("edit-methods", "m"), "Enable editing method names");
		parser.acceptsAll(List.of("no-edit-methods", "M"), "Disable editing method names");
		parser.acceptsAll(List.of("edit-fields", "f"), "Enable editing field names");
		parser.acceptsAll(List.of("no-edit-fields", "F"), "Disable editing field names");
		parser.acceptsAll(List.of("edit-parameters", "p"), "Enable editing parameter names");
		parser.acceptsAll(List.of("no-edit-parameters", "P"), "Disable editing parameter names");
		parser.acceptsAll(List.of("edit-locals"), "Enable editing local variable names");
		parser.acceptsAll(List.of("no-edit-locals"), "Disable editing local variable names");
		parser.acceptsAll(List.of("edit-javadocs", "d"), "Enable editing Javadocs");
		parser.acceptsAll(List.of("no-edit-javadocs", "D"), "Disable editing Javadocs");

		parser.accepts("development", "Enable extra options and information for development");

		parser.accepts("help", "Displays help information");

		try {
			OptionSet options = parser.parse(args);

			if (options.has("help")) {
				parser.printHelpOn(System.out);
				return;
			}

			if (options.has("development")) {
				System.setProperty("enigma.development", "true");
			}

			Set<EditableType> editables = EnumSet.allOf(EditableType.class);

			for (OptionSpec<?> spec : options.specs()) {
				for (String s : spec.options()) {
					switch (s) {
						case "edit-all" -> editables.addAll(List.of(EditableType.values()));
						case "no-edit-all" -> editables.clear();
						case "edit-classes" -> editables.add(EditableType.CLASS);
						case "no-edit-classes" -> editables.remove(EditableType.CLASS);
						case "edit-methods" -> editables.add(EditableType.METHOD);
						case "no-edit-methods" -> editables.remove(EditableType.METHOD);
						case "edit-fields" -> editables.add(EditableType.FIELD);
						case "no-edit-fields" -> editables.remove(EditableType.FIELD);
						case "edit-parameters" -> editables.add(EditableType.PARAMETER);
						case "no-edit-parameters" -> editables.remove(EditableType.PARAMETER);
						case "edit-locals" -> {
							editables.add(EditableType.LOCAL_VARIABLE);
							Logger.warn("--edit-locals has no effect as local variables are currently not editable");
						}
						case "no-edit-locals" -> {
							editables.remove(EditableType.LOCAL_VARIABLE);
							Logger.warn("--no-edit-locals has no effect as local variables are currently not editable");
						}
						case "edit-javadocs" -> editables.add(EditableType.JAVADOC);
						case "no-edit-javadocs" -> editables.remove(EditableType.JAVADOC);
					}
				}
			}

			EnigmaProfile parsedProfile = EnigmaProfile.read(options.valueOf(profile));

			I18n.setLanguage(Config.main().language.value());
			setDefaultSystemProperty("apple.laf.useScreenMenuBar", "true");
			setDefaultSystemProperty("awt.useSystemAAFontSettings", "on");
			setDefaultSystemProperty("swing.aatext", "true");

			ThemeUtil.setupTheme();

			KeyBinds.loadConfig();

			Gui gui = new Gui(parsedProfile, editables, true);
			GuiController controller = gui.getController();

			if (options.has("hide-progress-bars")) {
				gui.setShowsProgressBars(false);
			}

			if (Boolean.parseBoolean(System.getProperty("enigma.catchExceptions", "true"))) {
				// install a global exception handler to the event thread
				CrashDialog.init(gui);
				Thread.setDefaultUncaughtExceptionHandler((thread, t) -> {
					Logger.error(t, "Uncaught exception in thread {}", thread);
					CrashDialog.show(t);
				});
			}

			if (options.has(jar)) {
				Path jarPath = options.valueOf(jar);
				controller.openJar(jarPath)
						.whenComplete((v, t) -> {
							if (options.has(mappings)) {
								Path mappingsPath = options.valueOf(mappings);
								gui.getController().openMappings(mappingsPath);
								gui.getNotificationManager().notify(ParameterizedMessage.openedProject(jarPath.toString(), mappingsPath.toString()));
							} else {
								// search for mappings that are associated with the jar
								for (Config.RecentProject recentProject : Config.main().recentProjects.value()) {
									if (recentProject.getJarPath().equals(jarPath)) {
										gui.getNotificationManager().notify(ParameterizedMessage.openedProject(recentProject.jarPath(), recentProject.mappingsPath()));
										gui.getController().openMappings(recentProject.getMappingsPath());
										break;
									}
								}

								gui.getNotificationManager().notify(new ParameterizedMessage(Message.OPENED_JAR, jarPath.toString().substring(jarPath.toString().lastIndexOf("/"))));
							}
						});
			} else {
				gui.openMostRecentFiles();
			}
		} catch (OptionException e) {
			Logger.error("Invalid arguments: {}\n", e.getMessage());
			parser.printHelpOn(System.out);
		}
	}

	private static void setDefaultSystemProperty(String property, String value) {
		System.setProperty(property, System.getProperty(property, value));
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

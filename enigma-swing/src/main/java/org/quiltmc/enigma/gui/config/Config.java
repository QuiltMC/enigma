package org.quiltmc.enigma.gui.config;

import org.jspecify.annotations.Nullable;
import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.Alias;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.annotations.Processor;
import org.quiltmc.config.api.annotations.SerializedNameConvention;
import org.quiltmc.config.api.metadata.NamingSchemes;
import org.quiltmc.config.api.serializers.TomlSerializer;
import org.quiltmc.config.api.values.ComplexConfigValue;
import org.quiltmc.config.api.values.ConfigSerializableObject;
import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.config.api.values.ValueList;
import org.quiltmc.config.api.values.ValueMap;
import org.quiltmc.config.implementor_api.ConfigEnvironment;
import org.quiltmc.config.implementor_api.ConfigFactory;
import org.quiltmc.enigma.gui.NotificationManager;
import org.quiltmc.enigma.gui.config.theme.Theme;
import org.quiltmc.enigma.gui.config.theme.properties.DarcerulaThemeProperties;
import org.quiltmc.enigma.gui.config.theme.properties.DarculaThemeProperties;
import org.quiltmc.enigma.gui.config.theme.properties.DefaultThemeProperties;
import org.quiltmc.enigma.gui.config.theme.properties.MetalThemeProperties;
import org.quiltmc.enigma.gui.config.theme.properties.NoneThemeProperties;
import org.quiltmc.enigma.gui.config.theme.properties.SystemThemeProperties;
import org.quiltmc.enigma.gui.config.theme.properties.composite.SyntaxPaneProperties;
import org.quiltmc.enigma.util.I18n;

import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Dimension;
import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Enigma config is separated into several {@value #FORMAT} files with names matching the methods used to access them:
 * <ul>
 *     <li> {@link #main()} (this one)
 *     <li> {@link #net()} (networking)
 *     <li> {@link #keybind()}
 *     <li> {@link #docker()}
 *     <li> {@link #decompiler()}
 *     <li> {@link #editor()}
 * </ul>
 *
 * {@value #THEME_FAMILY} also holds a config file for each theme;
 * the active theme is accessible via {@link #currentTheme()}.
 */
@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
@Processor("processChange")
public final class Config extends ReflectiveConfig {
	private static final String FORMAT = "toml";
	private static final String FAMILY = "enigma";
	private static final String THEME_FAMILY = FAMILY + "/theme";

	private static final ConfigEnvironment ENVIRONMENT = new ConfigEnvironment(ConfigPaths.getConfigPathRoot(), FORMAT, TomlSerializer.INSTANCE);

	private static final Config MAIN = ConfigFactory.create(ENVIRONMENT, FAMILY, "main", Config.class);
	private static final KeyBindConfig KEYBIND = ConfigFactory.create(ENVIRONMENT, FAMILY, "keybind", KeyBindConfig.class);
	private static final NetConfig NET = ConfigFactory.create(ENVIRONMENT, FAMILY, "net", NetConfig.class);
	private static final DockerConfig DOCKER = ConfigFactory.create(ENVIRONMENT, FAMILY, "docker", DockerConfig.class);
	private static final DecompilerConfig DECOMPILER = ConfigFactory.create(ENVIRONMENT, FAMILY, "decompiler", DecompilerConfig.class);
	private static final EditorConfig EDITOR = ConfigFactory.create(ENVIRONMENT, FAMILY, "editor", EditorConfig.class);

	@Comment("The currently assigned UI language. This will be an ISO-639 two-letter language code, followed by an underscore and an ISO 3166-1 alpha-2 two-letter country code.")
	@Processor("grabPossibleLanguages")
	public final TrackedValue<String> language = this.value(I18n.DEFAULT_LANGUAGE);
	@Comment("A float representing the current size of the UI. 1.0 represents 100% scaling.")
	public final TrackedValue<Float> scaleFactor = this.value(1.0f);
	@Comment("The maximum number of saved recent projects, for quickly reopening.")
	public final TrackedValue<Integer> maxRecentProjects = this.value(10);
	public final TrackedValue<ValueList<RecentProject>> recentProjects = this.list(new RecentProject("", ""));
	@Comment("Modifies how many notifications you'll get while part of a multiplayer mapping server.")
	public final TrackedValue<NotificationManager.ServerNotificationLevel> serverNotificationLevel = this.value(NotificationManager.ServerNotificationLevel.FULL);
	@Comment("How big the Enigma window will open, in pixels.")
	public final TrackedValue<Vec2i> windowSize = this.value(new Vec2i(1024, 576));
	@Comment("The position the top-left corner of Enigma's window will be the next time it opens, in pixels.")
	public final TrackedValue<Vec2i> windowPos = this.value(new Vec2i(0, 0));

	@Comment("The settings for the statistics window.")
	public final StatsSection stats = new StatsSection();

	@Comment("You shouldn't enable options in this section unless you know what you're doing!")
	public final DevSection development = new DevSection();

	/**
	 * The look and feel stored in the config: do not use this unless setting! Use {@link #activeThemeChoice} instead,
	 * since look and feel is final once loaded.
	 */
	@Alias("look_and_feel")
	public final TrackedValue<ThemeChoice> theme = this.value(ThemeChoice.DEFAULT);
	/**
	 * Look and feel is not modifiable at runtime. I have tried and failed multiple times to get this running.
	 */
	public static ThemeChoice activeThemeChoice;

	public static void configureTheme() {
		currentTheme().configure();
	}

	@SuppressWarnings("unused")
	public void processChange(org.quiltmc.config.api.Config.Builder builder) {
		builder.callback(config -> {
			for (var value : config.values()) {
				if (value.key().length() > 1 && value.key().getKeyComponent(0).equals("development") && value.value().equals(true)) {
					this.development.anyEnabled = true;
				}
			}
		});
	}

	@SuppressWarnings("all")
	public static Config main() {
		return MAIN;
	}

	public static StatsSection stats() {
		return main().stats;
	}

	public static DockerConfig docker() {
		return DOCKER;
	}

	public static KeyBindConfig keybind() {
		return KEYBIND;
	}

	public static NetConfig net() {
		return NET;
	}

	public static DecompilerConfig decompiler() {
		return DECOMPILER;
	}

	public static EditorConfig editor() {
		return EDITOR;
	}

	public static Theme currentTheme() {
		return activeThemeChoice.theme;
	}

	public static SyntaxPaneProperties.Colors getCurrentSyntaxPaneColors() {
		return currentTheme().getSyntaxPaneColors();
	}

	public static void setGlobalLaf() {
		try {
			currentTheme().setGlobalLaf();
		} catch (UnsupportedLookAndFeelException | ClassNotFoundException
				| InstantiationException | IllegalAccessException e
		) {
			throw new Error("Failed to set global look and feel", e);
		}
	}

	public static Theme.Fonts currentFonts() {
		return currentTheme().fonts;
	}

	public static void insertRecentProject(String jarPath, String mappingsPath) {
		RecentProject project = new RecentProject(jarPath, mappingsPath);
		ValueList<RecentProject> projects = main().recentProjects.value();

		// add project, shifting to top if already on the list
		projects.remove(project);
		projects.add(0, new RecentProject(jarPath, mappingsPath));

		// remove the oldest project according to max values
		if (projects.size() > main().maxRecentProjects.value()) {
			projects.remove(projects.size() - 1);
		}
	}

	@Nullable
	public static RecentProject getMostRecentProject() {
		if (!main().recentProjects.value().isEmpty()) {
			return main().recentProjects.value().get(0);
		} else {
			return null;
		}
	}

	@SuppressWarnings("unused")
	public void grabPossibleLanguages(TrackedValue.Builder<String> builder) {
		String possibleLanguages = "Supported languages: " + String.join(", ", I18n.getAvailableLanguages().toArray(new String[0]));
		builder.metadata(Comment.TYPE, b -> b.add(possibleLanguages));
	}

	public record RecentProject(String jarPath, String mappingsPath) implements ConfigSerializableObject<ValueMap<String>> {
		public Path getJarPath() {
			return Paths.get(this.jarPath);
		}

		public Path getMappingsPath() {
			return Paths.get(this.mappingsPath);
		}

		@Override
		public RecentProject convertFrom(ValueMap<String> representation) {
			return new RecentProject(representation.get("jarPath"), representation.get("mappingsPath"));
		}

		@Override
		public ValueMap<String> getRepresentation() {
			return ValueMap.builder("")
				.put("jarPath", this.jarPath)
				.put("mappingsPath", this.mappingsPath)
				.build();
		}

		@Override
		public ComplexConfigValue copy() {
			return this;
		}
	}

	public record Vec2i(int x, int y) implements ConfigSerializableObject<ValueMap<Integer>> {
		public Dimension toDimension() {
			return new Dimension(this.x, this.y);
		}

		public static Vec2i fromDimension(Dimension dimension) {
			return new Vec2i(dimension.width, dimension.height);
		}

		public Point toPoint() {
			return new Point(this.x, this.y);
		}

		public static Vec2i fromPoint(Point point) {
			return new Vec2i(point.x, point.y);
		}

		@Override
		public Vec2i convertFrom(ValueMap<Integer> representation) {
			return new Vec2i(representation.get("x"), representation.get("y"));
		}

		@Override
		public ValueMap<Integer> getRepresentation() {
			return ValueMap.builder(0)
				.put("x", this.x)
				.put("y", this.y)
				.build();
		}

		@Override
		public ComplexConfigValue copy() {
			return this;
		}
	}

	public enum ThemeChoice implements ConfigSerializableObject<String> {
		DEFAULT(
			Theme.create(ENVIRONMENT, THEME_FAMILY, "default", new DefaultThemeProperties())
		),
		DARCULA(
			Theme.create(ENVIRONMENT, THEME_FAMILY, "darcula", new DarculaThemeProperties())
		),
		DARCERULA(
			Theme.create(ENVIRONMENT, THEME_FAMILY, "darcerula", new DarcerulaThemeProperties())
		),
		METAL(
			Theme.create(ENVIRONMENT, THEME_FAMILY, "metal", new MetalThemeProperties())
		),
		SYSTEM(
			Theme.create(ENVIRONMENT, THEME_FAMILY, "system", new SystemThemeProperties())
		),
		NONE(
			Theme.create(ENVIRONMENT, THEME_FAMILY, "none", new NoneThemeProperties())
		);

		private final Theme theme;

		ThemeChoice(Theme theme) {
			this.theme = theme;
		}

		@Override
		public ThemeChoice convertFrom(String representation) {
			return ThemeChoice.valueOf(representation);
		}

		@Override
		public String getRepresentation() {
			return this.name();
		}

		@Override
		public ComplexConfigValue copy() {
			return this;
		}
	}
}

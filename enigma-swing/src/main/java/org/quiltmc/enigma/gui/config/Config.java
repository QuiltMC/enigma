package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
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
import org.quiltmc.enigma.gui.config.theme.LookAndFeel;
import org.quiltmc.enigma.gui.config.theme.Theme;
import org.quiltmc.enigma.gui.dialog.EnigmaQuickFindDialog;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.syntaxpain.SyntaxpainConfiguration;

import javax.annotation.Nullable;
import java.awt.Dimension;
import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The Enigma config is separated into five different files: {@link Config the main config (this one)},
 * {@link NetConfig the networking configuration}, {@link KeyBindConfig the keybinding configuration},
 * {@link DockerConfig the docker configuration}, and {@link DecompilerConfig the decompiler configuration}.
 */
@SerializedNameConvention(NamingSchemes.SNAKE_CASE)
@Processor("processChange")
public final class Config extends ReflectiveConfig {
	private static final String FORMAT = "toml";
	private static final String FAMILY = "enigma";

	private static final ConfigEnvironment ENVIRONMENT = new ConfigEnvironment(ConfigPaths.getConfigPathRoot(), FORMAT, TomlSerializer.INSTANCE);
	private static final Config MAIN = ConfigFactory.create(ENVIRONMENT, FAMILY, "main", Config.class);
	private static final KeyBindConfig KEYBIND = ConfigFactory.create(ENVIRONMENT, FAMILY, "keybind", KeyBindConfig.class);
	private static final NetConfig NET = ConfigFactory.create(ENVIRONMENT, FAMILY, "net", NetConfig.class);
	private static final DockerConfig DOCKER = ConfigFactory.create(ENVIRONMENT, FAMILY, "docker", DockerConfig.class);
	private static final DecompilerConfig DECOMPILER = ConfigFactory.create(ENVIRONMENT, FAMILY, "decompiler", DecompilerConfig.class);

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

	@Comment("Contains all features that can be toggled on or off.")
	public final FeaturesSection features = new FeaturesSection();

	@Comment("You shouldn't enable options in this section unless you know what you're doing!")
	public final DevSection development = new DevSection();

	/**
	 * The look and feel stored in the config: do not use this unless setting! Use {@link #activeLookAndFeel} instead,
	 * since look and feel is final once loaded.
	 */
	public final TrackedValue<LookAndFeel> lookAndFeel = this.value(LookAndFeel.DEFAULT);
	/**
	 * Look and feel is not modifiable at runtime. I have tried and failed multiple times to get this running.
	 */
	public static LookAndFeel activeLookAndFeel;

	public final Theme defaultTheme = new Theme(LookAndFeel.DEFAULT);
	public final Theme darculaTheme = new Theme(LookAndFeel.DARCULA);
	public final Theme metalTheme = new Theme(LookAndFeel.METAL);
	public final Theme systemTheme = new Theme(LookAndFeel.SYSTEM);
	public final Theme noneTheme = new Theme(LookAndFeel.NONE);

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

	public static DockerConfig dockers() {
		return DOCKER;
	}

	public static KeyBindConfig keyBinds() {
		return KEYBIND;
	}

	public static NetConfig net() {
		return NET;
	}

	public static DecompilerConfig decompiler() {
		return DECOMPILER;
	}

	public static Theme currentTheme() {
		return switch (activeLookAndFeel) {
			case DEFAULT -> main().defaultTheme;
			case DARCULA -> main().darculaTheme;
			case METAL -> main().metalTheme;
			case SYSTEM -> main().systemTheme;
			case NONE -> main().noneTheme;
		};
	}

	public static Theme.Colors currentColors() {
		return currentTheme().colors;
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

	/**
	 * Updates the backend library Syntaxpain, used for code highlighting and other editor things.
	 */
	public static void updateSyntaxpain() {
		Theme.Fonts fonts = currentFonts();
		Theme.Colors colors = currentColors();

		SyntaxpainConfiguration.setEditorFont(fonts.editor.value());
		SyntaxpainConfiguration.setQuickFindDialogFactory(EnigmaQuickFindDialog::new);

		SyntaxpainConfiguration.setLineRulerPrimaryColor(colors.lineNumbersForeground.value());
		SyntaxpainConfiguration.setLineRulerSecondaryColor(colors.lineNumbersBackground.value());
		SyntaxpainConfiguration.setLineRulerSelectionColor(colors.lineNumbersSelected.value());

		SyntaxpainConfiguration.setHighlightColor(colors.highlight.value());
		SyntaxpainConfiguration.setStringColor(colors.string.value());
		SyntaxpainConfiguration.setNumberColor(colors.number.value());
		SyntaxpainConfiguration.setOperatorColor(colors.operator.value());
		SyntaxpainConfiguration.setDelimiterColor(colors.delimiter.value());
		SyntaxpainConfiguration.setTypeColor(colors.type.value());
		SyntaxpainConfiguration.setIdentifierColor(colors.identifier.value());
		SyntaxpainConfiguration.setCommentColour(colors.comment.value());
		SyntaxpainConfiguration.setTextColor(colors.text.value());
	}
}

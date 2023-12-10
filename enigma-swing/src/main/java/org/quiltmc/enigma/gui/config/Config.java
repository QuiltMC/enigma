package org.quiltmc.enigma.gui.config;

import org.quiltmc.config.api.ReflectiveConfig;
import org.quiltmc.config.api.annotations.SerializedName;
import org.quiltmc.config.api.serializer.TomlSerializer;
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

import java.awt.Dimension;
import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Config extends ReflectiveConfig {
	private static final String FORMAT = "toml";
	private static final String FAMILY = "enigma";

	private static final ConfigEnvironment ENVIRONMENT = new ConfigEnvironment(ConfigPaths.getConfigPathRoot(), FORMAT, TomlSerializer.INSTANCE);
	private static final Config MAIN = ConfigFactory.create(ENVIRONMENT, FAMILY, "main", Config.class);
	private static final KeyBindConfig KEYBIND = ConfigFactory.create(ENVIRONMENT, FAMILY, "keybind", KeyBindConfig.class);
	private static final NetConfig NET = ConfigFactory.create(ENVIRONMENT, FAMILY, "net", NetConfig.class);
	private static final DockerConfig DOCKER = ConfigFactory.create(ENVIRONMENT, FAMILY, "docker", DockerConfig.class);
	private static final DecompilerConfig DECOMPILER = ConfigFactory.create(ENVIRONMENT, FAMILY, "decompiler", DecompilerConfig.class);

	@SerializedName("language")
	public final TrackedValue<String> language = this.value(I18n.DEFAULT_LANGUAGE);
	@SerializedName("scale_factor")
	public final TrackedValue<Float> scaleFactor = this.value(1.0f);
	@SerializedName("max_recent_files")
	public final TrackedValue<Integer> maxRecentFiles = this.value(10);
	@SerializedName("recent_projects")
	public final TrackedValue<ValueList<RecentProject>> recentProjects = this.list(new RecentProject("", ""));
	@SerializedName("server_notification_level")
	public final TrackedValue<NotificationManager.ServerNotificationLevel> serverNotificationLevel = this.value(NotificationManager.ServerNotificationLevel.FULL);
	@SerializedName("use_custom_fonts")
	public final TrackedValue<Boolean> useCustomFonts = this.value(false);
	@SerializedName("window_size")
	public final TrackedValue<Vec2i> windowSize = this.value(new Vec2i(1024, 576));
	@SerializedName("window_pos")
	public final TrackedValue<Vec2i> windowPos = this.value(new Vec2i(0, 0));

	public final StatsSection stats = new StatsSection();

	/**
	 * The look and feel stored in the config: do not use this unless setting! Use {@link #activeLookAndFeel} instead,
	 * since look and feel is final once loaded.
	 */
	@SerializedName("look_and_feel")
	public final TrackedValue<LookAndFeel> lookAndFeel = this.value(LookAndFeel.DEFAULT);
	/**
	 * Look and feel is not modifiable at runtime. I have tried and failed multiple times to get this running.
	 */
	public static LookAndFeel activeLookAndFeel;

	@SerializedName("default_theme")
	public final Theme defaultTheme = new Theme(LookAndFeel.DEFAULT);
	@SerializedName("darcula_theme")
	public final Theme darculaTheme = new Theme(LookAndFeel.DARCULA);
	@SerializedName("metal_theme")
	public final Theme metalTheme = new Theme(LookAndFeel.METAL);
	@SerializedName("system_theme")
	public final Theme systemTheme = new Theme(LookAndFeel.SYSTEM);
	@SerializedName("none_theme")
	public final Theme noneTheme = new Theme(LookAndFeel.NONE);

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
		main().recentProjects.value().add(0, new RecentProject(jarPath, mappingsPath));
	}

	public static RecentProject getMostRecentProject() {
		return main().recentProjects.value().get(0);
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

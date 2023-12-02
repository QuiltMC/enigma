package org.quiltmc.enigma.gui.config;

import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import org.quiltmc.config.api.ReflectiveConfig;
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
	private static final ConfigEnvironment ENVIRONMENT = new ConfigEnvironment(ConfigPaths.getConfigPathRoot(), "toml", new NightConfigSerializer<>("toml", new TomlParser(), new TomlWriter()));
	private static final Config MAIN = ConfigFactory.create(ENVIRONMENT, "enigma", "main", Config.class);
	private static final KeyBindsConfig KEYBINDS = ConfigFactory.create(ENVIRONMENT, "enigma", "keybinds", KeyBindsConfig.class);
	private static final NetConfig NET = ConfigFactory.create(ENVIRONMENT, "enigma", "net", NetConfig.class);
	private static final DockerConfig DOCKER = ConfigFactory.create(ENVIRONMENT, "enigma", "docker", DockerConfig.class);
	private static final DecompilerConfig DECOMPILER = ConfigFactory.create(ENVIRONMENT, "enigma", "decompiler", DecompilerConfig.class);

	public Config() {
		//updateSyntaxpain();
	}

	public final TrackedValue<String> language = this.value(I18n.DEFAULT_LANGUAGE);
	public final TrackedValue<Float> scaleFactor = this.value(1.0f);
	public final TrackedValue<Integer> maxRecentFiles = this.value(10);
	public final TrackedValue<ValueList<RecentProject>> recentProjects = this.list(new RecentProject("", ""));
	public final TrackedValue<NotificationManager.ServerNotificationLevel> serverNotificationLevel = this.value(NotificationManager.ServerNotificationLevel.FULL);
	public final TrackedValue<Boolean> useCustomFonts = this.value(false);
	public final TrackedValue<Vec2i> windowSize = this.value(new Vec2i(1024, 576));
	public final TrackedValue<Vec2i> windowPos = this.value(new Vec2i(0, 0));
	public final TrackedValue<String> lastSelectedDir = this.value("");
	public final TrackedValue<String> lastTopLevelPackage = this.value("");
	public final TrackedValue<Boolean> shouldIncludeSyntheticParameters = this.value(false);

	public final TrackedValue<LookAndFeel> lookAndFeel = this.value(LookAndFeel.DEFAULT);
	// todo laf can't be changed while running
	public final transient LookAndFeel activeLookAndFeel = this.lookAndFeel.value();

	public final Theme defaultTheme = new Theme(LookAndFeel.DEFAULT);
	public final Theme darculaTheme = new Theme(LookAndFeel.DARCULA);
	public final Theme metalTheme = new Theme(LookAndFeel.METAL);
	public final Theme systemTheme = new Theme(LookAndFeel.SYSTEM);
	public final Theme noneTheme = new Theme(LookAndFeel.NONE);

	public static Config get() {
		return MAIN;
	}

	public static DockerConfig dockers() {
		return DOCKER;
	}

	public static KeyBindsConfig keyBinds() {
		return KEYBINDS;
	}

	public static NetConfig net() {
		return NET;
	}

	public static DecompilerConfig decompiler() {
		return DECOMPILER;
	}

	public static Theme currentTheme() {
		return switch (MAIN.activeLookAndFeel) {
			case DEFAULT -> MAIN.defaultTheme;
			case DARCULA -> MAIN.darculaTheme;
			case METAL -> MAIN.metalTheme;
			case SYSTEM -> MAIN.systemTheme;
			case NONE -> MAIN.noneTheme;
		};
	}

	public static Theme.Colors currentColors() {
		return currentTheme().colors;
	}

	public static Theme.Fonts currentFonts() {
		return currentTheme().fonts;
	}

	public static void insertRecentProject(String jarPath, String mappingsPath) {
		MAIN.recentProjects.value().add(0, new RecentProject(jarPath, mappingsPath));
	}

	public static RecentProject getMostRecentProject() {
		return MAIN.recentProjects.value().get(0);
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
	private static void updateSyntaxpain() {
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

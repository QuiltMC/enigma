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
import org.quiltmc.enigma.gui.config.theme.ThemeColors;
import org.quiltmc.enigma.gui.config.theme.ThemeFonts;
import org.quiltmc.enigma.gui.dialog.EnigmaQuickFindDialog;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.syntaxpain.SyntaxpainConfiguration;

import java.awt.Dimension;
import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Config extends ReflectiveConfig {
	private static final ConfigEnvironment ENVIRONMENT = new ConfigEnvironment(ConfigPaths.getConfigPathRoot(), "toml", new NightConfigSerializer<>("toml", new TomlParser(), new TomlWriter()));
	private static final Config INSTANCE = ConfigFactory.create(ENVIRONMENT, "enigma", "main", Config.class);

	public Config() {
		updateSyntaxpain();
	}

	public final TrackedValue<String> language = this.value(I18n.DEFAULT_LANGUAGE);
	public final TrackedValue<Float> scaleFactor = this.value(1.0f);
	public final TrackedValue<Integer> maxRecentFiles = this.value(10);
	public final TrackedValue<ValueList<RecentProject>> recentProjects = this.list(new RecentProject("", ""));
	public final TrackedValue<NotificationManager.ServerNotificationLevel> serverNotificationLevel = this.value(NotificationManager.ServerNotificationLevel.FULL);
	public final TrackedValue<Boolean> useCustomFonts = this.value(false);
	public final TrackedValue<Dimension> windowSize = this.value(ScaleUtil.getDimension(1024, 576));
	public final TrackedValue<Point> windowPos = this.value(new Point());
	public final TrackedValue<String> lastSelectedDir = this.value("");
	public final TrackedValue<String> lastTopLevelPackage = this.value("");
	public final TrackedValue<Boolean> shouldIncludeSyntheticParameters = this.value(false);
	public final TrackedValue<KeyBindsConfig> keyBinds = this.value(new KeyBindsConfig());
	public final TrackedValue<NetConfig> net = this.value(new NetConfig());
	public final TrackedValue<DecompilerConfig> decompiler = this.value(new DecompilerConfig());
	public final TrackedValue<DockerConfig> dockerConfig = this.value(new DockerConfig());

	public final TrackedValue<LookAndFeel> lookAndFeel = this.value(LookAndFeel.DEFAULT);
	// todo laf can't be changed while running
	public final LookAndFeel activeLookAndFeel = this.lookAndFeel.value();

	public final TrackedValue<Theme> defaultTheme = this.value(new Theme(LookAndFeel.DEFAULT));
	public final TrackedValue<Theme> darculaTheme = this.value(new Theme(LookAndFeel.DEFAULT));
	public final TrackedValue<Theme> metalTheme = this.value(new Theme(LookAndFeel.METAL));
	public final TrackedValue<Theme> systemTheme = this.value(new Theme(LookAndFeel.SYSTEM));
	public final TrackedValue<Theme> noneTheme = this.value(new Theme(LookAndFeel.NONE));

	public static Config get() {
		return INSTANCE;
	}

	public static DockerConfig dockers() {
		return INSTANCE.dockerConfig.value();
	}

	public static KeyBindsConfig keyBinds() {
		return INSTANCE.keyBinds.value();
	}

	public static NetConfig net() {
		return INSTANCE.net.value();
	}

	public static DecompilerConfig decompiler() {
		return INSTANCE.decompiler.value();
	}

	public static Theme currentTheme() {
		return switch (INSTANCE.activeLookAndFeel) {
			case DEFAULT -> INSTANCE.defaultTheme.value();
			case DARCULA -> INSTANCE.darculaTheme.value();
			case METAL -> INSTANCE.metalTheme.value();
			case SYSTEM -> INSTANCE.systemTheme.value();
			case NONE -> INSTANCE.noneTheme.value();
		};
	}

	public static ThemeColors currentColors() {
		return currentTheme().colors.value();
	}

	public static ThemeFonts currentFonts() {
		return currentTheme().fonts.value();
	}

	public static void insertRecentProject(String jarPath, String mappingsPath) {
		INSTANCE.recentProjects.value().add(0, new RecentProject(jarPath, mappingsPath));
	}

	public static RecentProject getMostRecentProject() {
		return INSTANCE.recentProjects.value().get(0);
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

	/**
	 * Updates the backend library Syntaxpain, used for code highlighting and other editor things.
	 */
	private static void updateSyntaxpain() {
		ThemeFonts fonts = currentFonts();
		ThemeColors colors = currentColors();

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
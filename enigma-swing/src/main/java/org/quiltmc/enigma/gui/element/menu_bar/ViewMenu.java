package org.quiltmc.enigma.gui.element.menu_bar;

import org.quiltmc.enigma.api.stats.StatType;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.NotificationManager;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.dialog.ChangeDialog;
import org.quiltmc.enigma.gui.dialog.FontDialog;
import org.quiltmc.enigma.gui.util.LanguageUtil;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Pair;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ViewMenu extends AbstractEnigmaMenu {
	private final Gui gui;

	private final JMenu themesMenu = new JMenu();
	private final JMenu languagesMenu = new JMenu();
	private final JMenu scaleMenu = new JMenu();
	private final JMenu notificationsMenu = new JMenu();
	private final JMenu statIconsMenu = new JMenu();
	private final JMenuItem fontItem = new JMenuItem();
	private final JMenuItem customScaleItem = new JMenuItem();

	public ViewMenu(Gui gui) {
		this.gui = gui;

		this.prepareThemesMenu();
		this.prepareLanguagesMenu();
		this.prepareScaleMenu();
		this.prepareNotificationsMenu();
		this.prepareStatIconsMenu();

		this.add(this.themesMenu);
		this.add(this.languagesMenu);
		this.add(this.notificationsMenu);
		this.scaleMenu.add(this.customScaleItem);
		this.add(this.scaleMenu);
		this.add(this.statIconsMenu);
		this.add(this.fontItem);

		this.customScaleItem.addActionListener(e -> this.onCustomScaleClicked());
		this.fontItem.addActionListener(e -> this.onFontClicked(this.gui));
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.view"));
		this.themesMenu.setText(I18n.translate("menu.view.themes"));
		this.notificationsMenu.setText(I18n.translate("menu.view.notifications"));
		this.languagesMenu.setText(I18n.translate("menu.view.languages"));
		this.scaleMenu.setText(I18n.translate("menu.view.scale"));
		this.statIconsMenu.setText(I18n.translate("menu.view.stat_icons"));
		this.fontItem.setText(I18n.translate("menu.view.font"));
		this.customScaleItem.setText(I18n.translate("menu.view.scale.custom"));
	}

	private void onCustomScaleClicked() {
		String answer = (String) JOptionPane.showInputDialog(this.gui.getFrame(), I18n.translate("menu.view.scale.custom.title"), I18n.translate("menu.view.scale.custom.title"),
			JOptionPane.QUESTION_MESSAGE, null, null, Double.toString(Config.main().scaleFactor.value() * 100));

		if (answer == null) {
			return;
		}

		float newScale = 1.0f;
		try {
			newScale = Float.parseFloat(answer) / 100f;
		} catch (NumberFormatException ignored) {
			// ignored!
		}

		ScaleUtil.setScaleFactor(newScale);
		ChangeDialog.show(this.gui.getFrame());
	}

	private void onFontClicked(Gui gui) {
		FontDialog.display(gui.getFrame());
	}

	private void prepareThemesMenu() {
		ButtonGroup themeGroup = new ButtonGroup();
		for (Config.ThemeChoice themeChoice : Config.ThemeChoice.values()) {
			JRadioButtonMenuItem themeButton = new JRadioButtonMenuItem(I18n.translate("menu.view.themes." + themeChoice.name().toLowerCase(Locale.ROOT)));
			themeGroup.add(themeButton);
			if (themeChoice.equals(Config.main().theme.value())) {
				themeButton.setSelected(true);
			}

			themeButton.addActionListener(e -> {
				Config.main().theme.setValue(themeChoice, true);
				ChangeDialog.show(this.gui.getFrame());
			});

			this.themesMenu.add(themeButton);
		}
	}

	private void prepareLanguagesMenu() {
		ButtonGroup languageGroup = new ButtonGroup();
		for (String lang : I18n.getAvailableLanguages()) {
			JRadioButtonMenuItem languageButton = new JRadioButtonMenuItem(I18n.getLanguageName(lang));
			languageGroup.add(languageButton);
			if (lang.equals(Config.main().language.value())) {
				languageButton.setSelected(true);
			}

			languageButton.addActionListener(event -> {
				Config.main().language.setValue(lang, true);
				I18n.setLanguage(lang);
				LanguageUtil.dispatchLanguageChange();
			});

			this.languagesMenu.add(languageButton);
		}
	}

	private void prepareScaleMenu() {
		ButtonGroup scaleGroup = new ButtonGroup();
		Map<Float, JRadioButtonMenuItem> scaleButtons = IntStream.of(100, 125, 150, 175, 200)
			.mapToObj(scaleFactor -> {
				float realScaleFactor = scaleFactor / 100f;
				JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(String.format("%d%%", scaleFactor));
				menuItem.addActionListener(event -> ScaleUtil.setScaleFactor(realScaleFactor));
				menuItem.addActionListener(event -> ChangeDialog.show(this.gui.getFrame()));
				scaleGroup.add(menuItem);
				this.scaleMenu.add(menuItem);
				return new Pair<>(realScaleFactor, menuItem);
			})
			.collect(Collectors.toMap(Pair::a, Pair::b));

		JRadioButtonMenuItem currentScaleButton = scaleButtons.get(Config.main().scaleFactor.value());
		if (currentScaleButton != null) {
			currentScaleButton.setSelected(true);
		}

		ScaleUtil.addListener((newScale, oldScale) -> {
			JRadioButtonMenuItem mi = scaleButtons.get(newScale);
			if (mi != null) {
				mi.setSelected(true);
			} else {
				scaleGroup.clearSelection();
			}
		});
	}

	private void prepareNotificationsMenu() {
		ButtonGroup notificationsGroup = new ButtonGroup();

		for (NotificationManager.ServerNotificationLevel level : NotificationManager.ServerNotificationLevel.values()) {
			JRadioButtonMenuItem notificationsButton = new JRadioButtonMenuItem(level.getText());
			notificationsGroup.add(notificationsButton);

			if (level.equals(Config.main().serverNotificationLevel.value())) {
				notificationsButton.setSelected(true);
			}

			notificationsButton.addActionListener(event -> Config.main().serverNotificationLevel.setValue(level, true));

			this.notificationsMenu.add(notificationsButton);
		}
	}

	private void prepareStatIconsMenu() {
		JMenu statTypes = new JMenu(I18n.translate("menu.view.stat_icons.included_types"));
		for (StatType statType : StatType.values()) {
			JCheckBoxMenuItem checkbox = new JCheckBoxMenuItem(statType.getName());
			checkbox.setSelected(Config.main().stats.includedStatTypes.value().contains(statType));
			checkbox.addActionListener(event -> {
				if (checkbox.isSelected() && !Config.stats().includedStatTypes.value().contains(statType)) {
					Config.stats().includedStatTypes.value().add(statType);
				} else {
					Config.stats().includedStatTypes.value().remove(statType);
				}

				ViewMenu.this.gui.getController().regenerateAndUpdateStatIcons();
			});

			statTypes.add(checkbox);
		}

		JCheckBoxMenuItem enableIcons = new JCheckBoxMenuItem(I18n.translate("menu.view.stat_icons.enable_icons"));
		JCheckBoxMenuItem includeSynthetic = new JCheckBoxMenuItem(I18n.translate("menu.view.stat_icons.include_synthetic"));
		JCheckBoxMenuItem countFallback = new JCheckBoxMenuItem(I18n.translate("menu.view.stat_icons.count_fallback"));

		enableIcons.setSelected(Config.main().features.enableClassTreeStatIcons.value());
		includeSynthetic.setSelected(Config.main().stats.shouldIncludeSyntheticParameters.value());
		countFallback.setSelected(Config.main().stats.shouldCountFallbackNames.value());

		enableIcons.addActionListener(event -> {
			Config.main().features.enableClassTreeStatIcons.setValue(enableIcons.isSelected());
			ViewMenu.this.gui.getController().regenerateAndUpdateStatIcons();
		});

		includeSynthetic.addActionListener(event -> {
			Config.main().stats.shouldIncludeSyntheticParameters.setValue(includeSynthetic.isSelected());
			ViewMenu.this.gui.getController().regenerateAndUpdateStatIcons();
		});

		countFallback.addActionListener(event -> {
			Config.main().stats.shouldCountFallbackNames.setValue(countFallback.isSelected());
			ViewMenu.this.gui.getController().regenerateAndUpdateStatIcons();
		});

		this.statIconsMenu.add(enableIcons);
		this.statIconsMenu.add(includeSynthetic);
		this.statIconsMenu.add(countFallback);
		this.statIconsMenu.add(statTypes);
	}
}

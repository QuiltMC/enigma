package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractEnigmaMenu;
import org.quiltmc.enigma.gui.util.LanguageUtil;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JRadioButtonMenuItem;
import java.util.HashMap;
import java.util.Map;

public class LanguagesMenu extends AbstractEnigmaMenu {
	private final Map<String, JRadioButtonMenuItem> languages = new HashMap<>();

	protected LanguagesMenu(Gui gui) {
		super(gui);

		for (String lang : I18n.getAvailableLanguages()) {
			JRadioButtonMenuItem languageButton = new JRadioButtonMenuItem(I18n.getLanguageName(lang));
			this.languages.put(lang, languageButton);
			this.add(languageButton);

			languageButton.addActionListener(e -> this.onLanguageButtonClicked(lang));
		}
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.view.languages"));

		for (String lang : I18n.getAvailableLanguages()) {
			this.languages.get(lang).setText(I18n.getLanguageName(lang));
		}
	}

	@Override
	public void updateState() {
		for (String lang : I18n.getAvailableLanguages()) {
			if (lang.equals(Config.main().language.value())) {
				this.languages.get(lang).setSelected(true);
			}
		}
	}

	private void onLanguageButtonClicked(String lang) {
		Config.main().language.setValue(lang, true);
		I18n.setLanguage(lang);
		LanguageUtil.dispatchLanguageChange();
	}
}

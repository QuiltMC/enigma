package org.quiltmc.enigma.gui.element;

import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.keybind.KeyBinds;
import org.quiltmc.enigma.gui.util.NumberInputDialog;
import org.quiltmc.enigma.gui.element.menu_bar.ConventionalSearchableElement;
import org.quiltmc.enigma.gui.element.menu_bar.Retranslatable;
import org.quiltmc.enigma.util.I18n;

import javax.swing.JMenuItem;

public class IntRangeConfigMenuItem extends JMenuItem implements ConventionalSearchableElement, Retranslatable {
	public static final String DIALOG_TITLE_TRANSLATION_KEY_SUFFIX = ".dialog_title";
	public static final String DIALOG_EXPLANATION_TRANSLATION_KEY_SUFFIX = ".dialog_explanation";
	private final TrackedValue<Integer> config;

	private final String translationKey;

	/**
	 * Constructs a menu item that, when clicked, prompts the user for an integer between the passed {@code min} and
	 * {@code max} using a dialog.<br>
	 * The menu item will be kept in sync with the passed {@code config}.
	 *
	 * @param gui                the gui
	 * @param config             the config value to sync with
	 * @param min                the minimum allowed value;
	 *                           this should coincide with any minimum imposed on the passed {@code config}
	 * @param max                the maximum allowed value
	 *                           this should coincide with any maximum imposed on the passed {@code config}
	 * @param defaultStep        the amount to step the value by when the user clicks step up/down buttons;
	 *                           must be positive
	 * @param altStep            the amount to step the value by when the user inputs {@link  KeyBinds#ALT_STEP_UP} or
	 *                           {@link KeyBinds#ALT_STEP_DOWN}; must be positive
	 * @param rootTranslationKey a translation key for deriving translations as follows:
	 *                           <ul>
	 *                               <li> this component's text: the unmodified key
	 *                               <li> the title of the dialog: the key with
	 *                                    {@value #DIALOG_TITLE_TRANSLATION_KEY_SUFFIX} appended
	 *                               <li> the explanation of the dialog: the key with
	 *                                    {@value #DIALOG_EXPLANATION_TRANSLATION_KEY_SUFFIX} appended
	 *                           </ul>
	 */
	public IntRangeConfigMenuItem(
			Gui gui, TrackedValue<Integer> config,
			int min, int max, int defaultStep, int altStep,
			String rootTranslationKey
	) {
		this(
				gui, config, min, max, defaultStep, altStep, rootTranslationKey,
				rootTranslationKey + DIALOG_TITLE_TRANSLATION_KEY_SUFFIX,
				rootTranslationKey + DIALOG_EXPLANATION_TRANSLATION_KEY_SUFFIX
		);
	}

	private IntRangeConfigMenuItem(
			Gui gui, TrackedValue<Integer> config, int min, int max, int defaultStep, int altStep,
			String translationKey, String dialogTitleTranslationKey, String dialogExplanationTranslationKey
	) {
		this.config = config;
		this.translationKey = translationKey;

		this.addActionListener(e -> {
			final String title = I18n.translate(dialogTitleTranslationKey);
			final String message = I18n.translate(dialogExplanationTranslationKey) + "\n"
					+ I18n.translateFormatted("prompt.input.int_range", min, max);
			final int input = NumberInputDialog.promptInt(
					gui.getFrame(), config.value(), min, max, defaultStep, altStep,
					title, message, I18n.translate("prompt.save")
			);

			if (!config.value().equals(input)) {
				config.setValue(input);
			}
		});

		config.registerCallback(updated -> {
			this.retranslate();
			gui.getMenuBar().clearSearchMenusResults();
		});
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translateFormatted(this.translationKey, this.config.value()));
	}

	@Override
	public String getAliasesTranslationKeyPrefix() {
		return this.translationKey;
	}

	@Override
	public String getSearchName() {
		return this.getText();
	}

	@Override
	public void onSearchChosen() {
		this.doClick(0);
	}
}

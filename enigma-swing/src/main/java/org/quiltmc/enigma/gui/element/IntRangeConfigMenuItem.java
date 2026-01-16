package org.quiltmc.enigma.gui.element;

import org.quiltmc.config.api.values.TrackedValue;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.util.I18n;
import org.quiltmc.enigma.util.Utils;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.util.Optional;

public class IntRangeConfigMenuItem extends JMenuItem {
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
	 * @param rootTranslationKey a translation key for deriving translations as follows:
	 *                           <ul>
	 *                               <li> this component's text: the unmodified key
	 *                               <li> the title of the dialog: the key with
	 *                                    {@value #DIALOG_TITLE_TRANSLATION_KEY_SUFFIX} appended
	 *                               <li> the explanation of the dialog: the key with
	 *                                    {@value #DIALOG_EXPLANATION_TRANSLATION_KEY_SUFFIX} appended
	 *                           </ul>
	 */
	public IntRangeConfigMenuItem(Gui gui, TrackedValue<Integer> config, int min, int max, String rootTranslationKey) {
		this(
				gui, config, min, max, rootTranslationKey,
				rootTranslationKey + DIALOG_TITLE_TRANSLATION_KEY_SUFFIX,
				rootTranslationKey + DIALOG_EXPLANATION_TRANSLATION_KEY_SUFFIX
		);
	}

	private IntRangeConfigMenuItem(
			Gui gui, TrackedValue<Integer> config, int min, int max,
			String translationKey, String dialogTitleTranslationKey, String dialogExplanationTranslationKey
	) {
		this.config = config;
		this.translationKey = translationKey;

		this.addActionListener(e ->
				getRangedIntInput(
					gui, config.value(), min, max,
					I18n.translate(dialogTitleTranslationKey),
					I18n.translate(dialogExplanationTranslationKey)
				)
				.ifPresent(input -> {
					if (!input.equals(config.value())) {
						config.setValue(input);
					}
				})
		);

		config.registerCallback(updated -> {
			this.retranslate();
		});
	}

	public void retranslate() {
		this.setText(I18n.translateFormatted(this.translationKey, this.config.value()));
	}

	private static Optional<Integer> getRangedIntInput(
			Gui gui, int initialValue, int min, int max, String title, String explanation
	) {
		final String prompt = I18n.translateFormatted("prompt.input.int_range", min, max);
		final String input = (String) JOptionPane.showInputDialog(
				gui.getFrame(),
				explanation + "\n" + prompt,
				title,
				JOptionPane.QUESTION_MESSAGE, null, null, initialValue
		);

		if (input != null) {
			try {
				return Optional.of(Utils.clamp(Integer.parseInt(input), min, max));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}
	}
}

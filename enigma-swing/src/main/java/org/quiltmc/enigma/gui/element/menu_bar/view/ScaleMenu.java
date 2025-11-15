package org.quiltmc.enigma.gui.element.menu_bar.view;

import org.quiltmc.enigma.gui.ConnectionState;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.config.Config;
import org.quiltmc.enigma.gui.dialog.ChangeDialog;
import org.quiltmc.enigma.gui.element.menu_bar.AbstractSearchableEnigmaMenu;
import org.quiltmc.enigma.gui.util.ScaleUtil;
import org.quiltmc.enigma.util.I18n;

import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

public class ScaleMenu extends AbstractSearchableEnigmaMenu {
	private final int[] defaultOptions = {100, 125, 150, 175, 200};
	private final ButtonGroup optionsGroup = new ButtonGroup();
	private final Map<Float, JRadioButtonMenuItem> options = new HashMap<>();
	private final JRadioButtonMenuItem customScaleButton = new JRadioButtonMenuItem();

	protected ScaleMenu(Gui gui) {
		super(gui);

		this.add(this.customScaleButton);
		this.optionsGroup.add(this.customScaleButton);

		this.forEachDefaultScaleOption((scaleFactor, realFactor) -> {
			JRadioButtonMenuItem button = new JRadioButtonMenuItem();

			this.optionsGroup.add(button);
			this.options.put(realFactor, button);
			this.add(button);

			button.addActionListener(e -> this.onScaleClicked(realFactor));
		});

		this.customScaleButton.addActionListener(e -> this.onCustomScaleClicked());

		// note: as of refactoring this code, there is no other path which updates scale
		// this code is therefore currently pointless
		// and exists only for a possible future in which some other code path is updating scale *without* calling Gui#updateUiState
		ScaleUtil.addListener((newScale, oldScale) -> this.updateState());
	}

	@Override
	public void retranslate() {
		this.setText(I18n.translate("menu.view.scale"));

		this.customScaleButton.setText(I18n.translate("menu.view.scale.custom"));
		this.forEachDefaultScaleOption((scaleFactor, realFactor) -> this.options.get(realFactor).setText(String.format("%d%%", scaleFactor)));
	}

	@Override
	public void updateState(boolean jarOpen, ConnectionState state) {
		JRadioButtonMenuItem option = this.options.get(Config.main().scaleFactor.value());
		Objects.requireNonNullElse(option, this.customScaleButton).setSelected(true);
	}

	private void onScaleClicked(float realScale) {
		ScaleUtil.setScaleFactor(realScale);
		ChangeDialog.show(this.gui.getFrame());
	}

	private void onCustomScaleClicked() {
		String answer = (String) JOptionPane.showInputDialog(this.gui.getFrame(), I18n.translate("menu.view.scale.custom.title"), I18n.translate("menu.view.scale.custom.title"),
				JOptionPane.QUESTION_MESSAGE, null, null, Double.toString(Config.main().scaleFactor.value() * 100));

		if (answer == null) {
			// cancelled
			// button is considered selected, we need to go back to the old selection
			this.updateState();
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

		// if custom scale matches a default scale, select that instead
		this.updateState();
	}

	private void forEachDefaultScaleOption(BiConsumer<Integer, Float> consumer) {
		IntStream.of(this.defaultOptions)
				.forEach(
					option -> consumer.accept(option, option / 100f)
				);
	}
}

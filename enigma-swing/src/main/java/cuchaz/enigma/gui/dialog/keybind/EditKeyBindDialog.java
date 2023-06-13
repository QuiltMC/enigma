package cuchaz.enigma.gui.dialog.keybind;

import cuchaz.enigma.gui.config.keybind.KeyBind;
import cuchaz.enigma.gui.config.keybind.KeyBinds;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

public class EditKeyBindDialog extends JDialog {
	private final Frame owner;
	private final List<CombinationPanel> combinationPanels = new ArrayList<>();
	private final List<KeyBind.Combination> combinations;
	private final KeyBind keyBind;
	private final JPanel combinationsPanel;

	public EditKeyBindDialog(Frame owner, KeyBind bind) {
		super(owner, I18n.translate("menu.file.configure_keybinds.edit.title"), true);
		this.owner = owner;
		this.keyBind = bind;
		this.combinations = new ArrayList<>(this.keyBind.combinations());

		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout());

		// Add buttons
		JPanel buttonsPanel = new JPanel(new GridLayout(0, 2));
		JButton addButton = new JButton(I18n.translate("menu.file.configure_keybinds.edit.add"));
		addButton.addActionListener(e -> this.addCombination());
		addButton.addMouseListener(this.mouseListener());
		buttonsPanel.add(addButton);
		JButton clearButton = new JButton(I18n.translate("menu.file.configure_keybinds.edit.clear"));
		clearButton.addActionListener(e -> this.clearCombinations());
		clearButton.addMouseListener(this.mouseListener());
		buttonsPanel.add(clearButton);
		JButton resetButton = new JButton(I18n.translate("menu.file.configure_keybinds.edit.reset"));
		resetButton.addActionListener(e -> this.reset());
		resetButton.addMouseListener(this.mouseListener());
		buttonsPanel.add(resetButton);
		contentPane.add(buttonsPanel, BorderLayout.NORTH);

		// Add combinations panel
		this.combinationsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
		this.combinationsPanel.setBorder(new EmptyBorder(ScaleUtil.scale(10), ScaleUtil.scale(10), ScaleUtil.scale(10), ScaleUtil.scale(10)));
		this.combinationsPanel.addMouseListener(this.mouseListener());
		for (KeyBind.Combination combination : this.keyBind.combinations()) {
			CombinationPanel combinationPanel = new CombinationPanel(this, combination);
			combinationPanel.addMouseListener(this.mouseListener());
			this.combinationPanels.add(combinationPanel);
			this.combinationsPanel.add(combinationPanel);
		}

		contentPane.add(this.combinationsPanel, BorderLayout.CENTER);

		// Add confirmation buttons
		Container buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, ScaleUtil.scale(4), ScaleUtil.scale(4)));
		JButton saveButton = new JButton(I18n.translate("menu.file.configure_keybinds.save"));
		saveButton.addActionListener(event -> this.save());
		buttonContainer.add(saveButton);
		JButton cancelButton = new JButton(I18n.translate("prompt.cancel"));
		cancelButton.addActionListener(event -> this.cancel());
		buttonContainer.add(cancelButton);
		contentPane.add(buttonContainer, BorderLayout.SOUTH);

		this.addMouseListener(this.mouseListener());

		this.pack();
		this.setLocationRelativeTo(owner);
	}

	private void save() {
		boolean modified = !this.combinations.equals(this.keyBind.combinations());
		for (CombinationPanel combinationPanel : this.combinationPanels) {
			if (combinationPanel.isModified() && combinationPanel.isCombinationValid()) {
				modified = true;
				KeyBind.Combination combination = combinationPanel.getResultCombination();

				// using space as a keybind is not allowed, due to issues with it not working depending on where you click
				if (combination.keyCode() == KeyEvent.VK_SPACE) {
					JOptionPane.showMessageDialog(this.owner, I18n.translateFormatted("menu.file.configure_keybinds.invalid_character_error", "space"), I18n.translate("prompt.error"), JOptionPane.ERROR_MESSAGE);
					return;
				}

				if (this.isNewCombination(combinationPanel)) {
					this.combinations.add(combination);
				} else {
					int index = this.combinations.indexOf(combinationPanel.getOriginalCombination());
					if (index >= 0) {
						this.combinations.set(index, combination);
					} else {
						this.combinations.add(combination);
					}
				}
			}
		}

		if (modified) {
			this.keyBind.combinations().clear();
			this.keyBind.combinations().addAll(this.combinations);
		}

		this.setVisible(false);
		this.dispose();
	}

	private void cancel() {
		this.setVisible(false);
		this.dispose();
	}

	// Stop editing when the user clicks
	private MouseListener mouseListener() {
		return GuiUtil.onMouseClick(e -> this.stopEditing(null));
	}

	protected void removeCombination(CombinationPanel combinationPanel) {
		this.combinations.remove(combinationPanel.getOriginalCombination());
		this.combinationsPanel.remove(combinationPanel);
		this.combinationPanels.remove(combinationPanel);
		this.pack();
	}

	private void addCombination() {
		CombinationPanel combinationPanel = CombinationPanel.createEmpty(this);
		combinationPanel.addMouseListener(this.mouseListener());
		this.combinationsPanel.add(combinationPanel);
		this.combinationPanels.add(combinationPanel);
		this.pack();
	}

	private void clearCombinations() {
		for (CombinationPanel combinationPanel : this.combinationPanels) {
			this.combinations.remove(combinationPanel.getOriginalCombination());
			this.combinationsPanel.remove(combinationPanel);
		}

		this.combinationPanels.clear();
		this.pack();
	}

	private void reset() {
		this.combinations.clear();
		this.combinationPanels.clear();
		this.combinationsPanel.removeAll();

		KeyBinds.resetToDefault(this.keyBind);
		this.combinations.addAll(this.keyBind.combinations());
		for (KeyBind.Combination combination : this.combinations) {
			CombinationPanel combinationPanel = new CombinationPanel(this, combination);
			combinationPanel.addMouseListener(this.mouseListener());
			this.combinationPanels.add(combinationPanel);
			this.combinationsPanel.add(combinationPanel);
		}

		this.pack();
	}

	private boolean isNewCombination(CombinationPanel panel) {
		return panel.getOriginalCombination() != KeyBind.Combination.EMPTY;
	}

	// Stop editing all combination panels but the excluded one
	protected void stopEditing(CombinationPanel excluded) {
		for (CombinationPanel combinationPanel : this.combinationPanels) {
			if (combinationPanel == excluded) continue;
			combinationPanel.stopEditing();
		}
	}
}

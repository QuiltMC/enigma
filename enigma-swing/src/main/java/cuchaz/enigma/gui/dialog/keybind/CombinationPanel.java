package cuchaz.enigma.gui.dialog.keybind;

import cuchaz.enigma.gui.config.keybind.KeyBind;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.util.List;

public class CombinationPanel extends JPanel {
	private static final List<Integer> MODIFIER_KEYS = List.of(KeyEvent.VK_SHIFT, KeyEvent.VK_CONTROL, KeyEvent.VK_ALT, KeyEvent.VK_META);
	private static final List<Integer> MODIFIER_FLAGS = List.of(InputEvent.SHIFT_DOWN_MASK, InputEvent.CTRL_DOWN_MASK, InputEvent.ALT_DOWN_MASK, InputEvent.META_DOWN_MASK);
	private static final Color EDITING_BUTTON_FOREGROUND = Color.ORANGE;
	private final EditKeyBindDialog parent;
	private final JButton button;
	private final Color defaultButtonFg;
	private final KeyBind.Combination originalCombination;
	private final MutableCombination editingCombination;
	private final MutableCombination lastCombination;
	private boolean editing = false;

	public CombinationPanel(EditKeyBindDialog parent, KeyBind.Combination combination) {
		this.parent = parent;
		this.originalCombination = combination;
		this.editingCombination = MutableCombination.fromCombination(combination);
		this.lastCombination = this.editingCombination.copy();

		this.setLayout(new FlowLayout(FlowLayout.RIGHT));
		this.setBorder(new EmptyBorder(0, ScaleUtil.scale(15), 0, ScaleUtil.scale(15)));

		JButton removeButton = new JButton(I18n.translate("menu.file.configure_keybinds.edit.remove"));
		removeButton.addActionListener(e -> this.parent.removeCombination(this));
		removeButton.addMouseListener(this.mouseListener());
		this.add(removeButton);

		this.button = new JButton(this.getButtonText());
		this.defaultButtonFg = this.button.getForeground();
		this.button.addActionListener(e -> this.onButtonPressed());
		this.button.addMouseListener(this.mouseListener());
		this.button.addKeyListener(GuiUtil.onKeyPress(this::onKeyPressed));
		this.add(this.button);
	}

	private String getButtonText() {
		return this.editingCombination.toString();
	}

	private void onButtonPressed() {
		if (this.editing) {
			this.stopEditing();
		} else {
			this.startEditing();
		}
	}

	protected void stopEditing() {
		if (this.editing) {
			this.editing = false;
			this.button.setForeground(this.defaultButtonFg);

			if (!this.editingCombination.isEmpty() && !this.editingCombination.isValid()) {
				// Reset combination to last one if invalid
				this.editingCombination.setFrom(this.lastCombination);
				this.update();
			} else {
				this.lastCombination.setFrom(this.editingCombination);
			}
		}
	}

	private void startEditing() {
		if (!this.editing) {
			this.editing = true;
			this.button.setForeground(EDITING_BUTTON_FOREGROUND);
		}
	}

	private void update() {
		this.button.setText(this.getButtonText());
		this.parent.pack();
	}

	private void onKeyPressed(KeyEvent e) {
		if (this.editing) {
			if (MODIFIER_KEYS.contains(e.getKeyCode())) {
				int modifierIndex = MODIFIER_KEYS.indexOf(e.getKeyCode());
				int modifier = MODIFIER_FLAGS.get(modifierIndex);
				this.editingCombination.setKeyModifiers(this.editingCombination.keyModifiers | modifier);
			} else {
				this.editingCombination.setKeyCode(e.getKeyCode());
			}

			this.update();
		}
	}

	// Stop editing other CombinationPanels when clicking on this panel
	private MouseListener mouseListener() {
		return GuiUtil.onMouseClick(e -> this.parent.stopEditing(CombinationPanel.this));
	}

	public boolean isModified() {
		return !this.editingCombination.isSameCombination(this.originalCombination);
	}

	public boolean isCombinationValid() {
		return this.editingCombination.isValid();
	}

	public KeyBind.Combination getOriginalCombination() {
		return this.originalCombination;
	}

	public KeyBind.Combination getResultCombination() {
		return new KeyBind.Combination(this.editingCombination.keyCode, this.editingCombination.keyModifiers);
	}

	public static CombinationPanel createEmpty(EditKeyBindDialog parent) {
		return new CombinationPanel(parent, KeyBind.Combination.EMPTY);
	}

	private static class MutableCombination {
		private int keyCode;
		private int keyModifiers;

		private MutableCombination(int keyCode, int keyModifiers) {
			this.keyCode = keyCode;
			this.keyModifiers = keyModifiers;
		}

		public static MutableCombination fromCombination(KeyBind.Combination combination) {
			return new MutableCombination(combination.keyCode(), combination.keyModifiers());
		}

		public void setFrom(MutableCombination combination) {
			this.set(combination.getKeyCode(), combination.getKeyModifiers());
		}

		public void set(int keyCode, int keyModifiers) {
			this.keyCode = keyCode;
			this.keyModifiers = keyModifiers;
		}

		public int getKeyCode() {
			return this.keyCode;
		}

		public int getKeyModifiers() {
			return this.keyModifiers;
		}

		public void setKeyCode(int keyCode) {
			this.keyCode = keyCode;
		}

		public void setKeyModifiers(int keyModifiers) {
			this.keyModifiers = keyModifiers;
		}

		public MutableCombination copy() {
			return new MutableCombination(this.keyCode, this.keyModifiers);
		}

		@Override
		public String toString() {
			String modifiers = this.modifiersToString();
			String key = this.keyToString();
			if (!modifiers.isEmpty()) {
				return modifiers + "+" + key;
			}

			return key;
		}

		private String modifiersToString() {
			if (this.keyModifiers == 0) {
				return "";
			}

			return InputEvent.getModifiersExText(this.keyModifiers);
		}

		private String keyToString() {
			if (this.keyCode == -1) {
				return I18n.translate("menu.file.configure_keybinds.edit.empty");
			}

			return KeyEvent.getKeyText(this.keyCode);
		}

		public boolean isEmpty() {
			return this.keyCode == -1 && this.keyModifiers == 0;
		}

		public boolean isValid() {
			return this.keyCode != -1;
		}

		public boolean isSameCombination(Object obj) {
			if (obj instanceof KeyBind.Combination combination) {
				return combination.keyCode() == this.keyCode && combination.keyModifiers() == this.keyModifiers;
			} else if (obj instanceof MutableCombination mutableCombination) {
				return mutableCombination.keyCode == this.keyCode && mutableCombination.keyModifiers == this.keyModifiers;
			}

			return false;
		}
	}
}

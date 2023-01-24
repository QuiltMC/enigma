package cuchaz.enigma.gui.dialog.keybind;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.keybind.KeyBind;
import cuchaz.enigma.gui.config.keybind.KeyBinds;
import cuchaz.enigma.gui.util.GridBagConstraintsBuilder;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Map;

public class ConfigureKeyBindsDialog extends JDialog {
	private final Gui gui;

	public ConfigureKeyBindsDialog(Gui gui) {
		super(gui.getFrame(), I18n.translate("menu.file.configure_keybinds.title"), true);
		this.gui = gui;
		JFrame owner = gui.getFrame();

		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout());

		// Add categories
		JPanel categoriesPanel = new JPanel(new GridBagLayout());
		categoriesPanel.setBorder(new EmptyBorder(ScaleUtil.scale(10), ScaleUtil.scale(10), ScaleUtil.scale(10), ScaleUtil.scale(10)));
		Map<String, List<KeyBind>> keyBinds = KeyBinds.getEditableKeyBindsByCategory();
		int i = 0;
		for (Map.Entry<String, List<KeyBind>> entry : keyBinds.entrySet()) {
			String category = entry.getKey();
			if (category.isEmpty()) {
				// keys in the empty category can't be configured
				continue;
			}

			JLabel label = new JLabel(I18n.translate("keybind.category." + category));
			JButton button = new JButton(I18n.translate("menu.file.configure_keybinds.edit"));
			button.addActionListener(e -> {
				ConfigureCategoryKeyBindsDialog dialog = new ConfigureCategoryKeyBindsDialog(owner, category, entry.getValue());
				dialog.setVisible(true);
			});

			GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(2);

			categoriesPanel.add(label, cb.pos(0, i).weightX(0.0).anchor(GridBagConstraints.LINE_END).fill(GridBagConstraints.NONE).build());
			categoriesPanel.add(button, cb.pos(1, i).weightX(1.0).anchor(GridBagConstraints.LINE_END).fill(GridBagConstraints.HORIZONTAL).build());
			i++;
		}
		contentPane.add(categoriesPanel, BorderLayout.CENTER);

		// Add buttons
		Container buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, ScaleUtil.scale(4), ScaleUtil.scale(4)));
		JButton saveButton = new JButton(I18n.translate("menu.file.configure_keybinds.save"));
		saveButton.addActionListener(event -> this.save());
		buttonContainer.add(saveButton);
		JButton cancelButton = new JButton(I18n.translate("prompt.cancel"));
		cancelButton.addActionListener(event -> this.cancel());
		buttonContainer.add(cancelButton);
		contentPane.add(buttonContainer, BorderLayout.SOUTH);

		this.addWindowListener(GuiUtil.onWindowClose(e -> KeyBinds.resetEditableKeyBinds()));

		this.pack();
		this.setLocationRelativeTo(owner);
	}

	private void save() {
		KeyBinds.saveConfig();
		this.gui.reloadKeyBinds();
		this.setVisible(false);
		this.dispose();
	}

	private void cancel() {
		KeyBinds.resetEditableKeyBinds();
		this.setVisible(false);
		this.dispose();
	}

	public static void show(Gui gui) {
		ConfigureKeyBindsDialog dialog = new ConfigureKeyBindsDialog(gui);
		dialog.setVisible(true);
	}
}

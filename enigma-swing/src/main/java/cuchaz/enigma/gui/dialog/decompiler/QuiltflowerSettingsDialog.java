package cuchaz.enigma.gui.dialog.decompiler;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.DecompilerConfig;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.source.quiltflower.QuiltflowerPreferences;
import cuchaz.enigma.utils.I18n;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;

public class QuiltflowerSettingsDialog extends JDialog {
	private final Map<String, Object> options = new HashMap<>(QuiltflowerPreferences.OPTIONS);

	public QuiltflowerSettingsDialog(Gui gui, JDialog parent) {
		super(parent, I18n.translate("menu.decompiler.settings.quiltflower"), true);
		Container pane = this.getContentPane();
		pane.setLayout(new BorderLayout());

		JPanel preferencesPanel = new JPanel();
		preferencesPanel.setLayout(new BoxLayout(preferencesPanel, BoxLayout.Y_AXIS));

		JScrollPane preferencesScrollPanel = new JScrollPane(preferencesPanel);
		preferencesScrollPanel.setPreferredSize(new Dimension(ScaleUtil.scale(480), ScaleUtil.scale(480)));
		preferencesScrollPanel.setBorder(new EmptyBorder(ScaleUtil.scale(10), ScaleUtil.scale(20), ScaleUtil.scale(10), ScaleUtil.scale(20)));
		preferencesScrollPanel.getVerticalScrollBar().setUnitIncrement(16);

		for (QuiltflowerPreferences.Preference preference : QuiltflowerPreferences.getPreferences()) {
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			JLabel label = new JLabel(preference.name());
			label.setToolTipText(preference.description());

			String key = preference.key();
			Object value = QuiltflowerPreferences.getValue(key);
			JComponent input = switch (preference.type()) {
				case BOOLEAN -> {
					JCheckBox c = new JCheckBox();
					c.setSelected(value.equals("1"));
					c.addActionListener(e -> {
						String newValue = c.isSelected() ? "1" : "0";
						if (newValue != QuiltflowerPreferences.DEFAULTS.get(key)) {
							this.options.put(key, newValue);
						} else {
							this.options.remove(key);
						}
					});
					yield c;
				}
				case STRING -> {
					JTextField t = new JTextField((String) value);
					t.setColumns(20);
					t.addActionListener(e -> {
						String newValue = t.getText();
						if (newValue != QuiltflowerPreferences.DEFAULTS.get(key)) {
							this.options.put(key, newValue);
						} else {
							this.options.remove(key);
						}
					});
					yield t;
				}
				case INTEGER -> {
					JSpinner spinner = new JSpinner();
					spinner.setModel(new SpinnerNumberModel(toInt(value), 0, Integer.MAX_VALUE, 1));
					spinner.addChangeListener(e -> {
						Object newValue = spinner.getValue();
						if (newValue != QuiltflowerPreferences.DEFAULTS.get(key)) {
							this.options.put(key, newValue);
						} else {
							this.options.remove(key);
						}
					});
					yield spinner;
				}
			};
			input.setMaximumSize(new Dimension(ScaleUtil.scale(100), ScaleUtil.scale(20)));


			if (input instanceof JCheckBox) {
				panel.add(input);
				panel.add(label);
			} else {
				panel.add(label);
				panel.add(input);
			}

			preferencesPanel.add(panel);
		}

		pane.add(preferencesScrollPanel, BorderLayout.CENTER);


		Container buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, ScaleUtil.scale(4), ScaleUtil.scale(4)));
		JButton saveButton = new JButton(I18n.translate("prompt.save"));
		saveButton.addActionListener(event -> this.save());
		buttonContainer.add(saveButton);
		JButton cancelButton = new JButton(I18n.translate("prompt.cancel"));
		cancelButton.addActionListener(event -> this.dispose());
		buttonContainer.add(cancelButton);
		pane.add(buttonContainer, BorderLayout.SOUTH);

		this.pack();
		this.setLocationRelativeTo(gui.getFrame());
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}

	private static int toInt(Object val) {
		if (val instanceof Integer) {
			return (Integer) val;
		}

		try {
			return Integer.parseInt(val.toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void save() {
		QuiltflowerPreferences.OPTIONS.clear();
		QuiltflowerPreferences.OPTIONS.putAll(this.options);

		DecompilerConfig.updateQuiltflowerValues(QuiltflowerPreferences.OPTIONS);
		DecompilerConfig.save();
		this.dispose();
	}
}

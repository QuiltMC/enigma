package cuchaz.enigma.gui.dialog;

import cuchaz.enigma.gui.config.KeyBindsConfig;
import cuchaz.enigma.gui.config.keybind.KeyBind;
import cuchaz.enigma.gui.config.keybind.KeyBinds;
import cuchaz.enigma.gui.util.GridBagConstraintsBuilder;
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
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigureKeyBindsDialog extends JDialog {
    private final List<ConfigureCategoryKeyBindsDialog> categoryDialogs = new ArrayList<>();

    public ConfigureKeyBindsDialog(Frame owner) {
        super(owner, I18n.translate("menu.file.configure_keybinds.title"), true);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        JLabel warningLabel = new JLabel(I18n.translate("menu.file.configure_keybinds.warning"));
        contentPane.add(warningLabel, BorderLayout.NORTH);

        JPanel inputContainer = new JPanel(new GridBagLayout());
        inputContainer.setBorder(new EmptyBorder(ScaleUtil.scale(10), ScaleUtil.scale(10), ScaleUtil.scale(10), ScaleUtil.scale(10)));

        Map<String, List<KeyBind>> keyBindsByCategory = KeyBinds.getConfigurableKeyBindsByCategory();
        List<Map.Entry<String, List<KeyBind>>> keyBindsByCategoryEntries = new ArrayList<>(keyBindsByCategory.entrySet());

        for (int i = 0; i < keyBindsByCategoryEntries.size(); i++) {
            Map.Entry<String, List<KeyBind>> entry = keyBindsByCategoryEntries.get(i);
            if (entry.getKey().isEmpty()) continue;

            JLabel label = new JLabel(I18n.translate("keybind.category." + entry.getKey()));
            JButton button = new JButton(I18n.translate("menu.file.configure_keybinds.edit"));
            button.addActionListener(e -> {
                ConfigureCategoryKeyBindsDialog dialog = new ConfigureCategoryKeyBindsDialog(owner, entry.getKey(), entry.getValue());
                categoryDialogs.add(dialog);
                dialog.setVisible(true);
            });

            GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(2);

            inputContainer.add(label, cb.pos(0, i).weightX(0.0).anchor(GridBagConstraints.LINE_END).fill(GridBagConstraints.NONE).build());
            inputContainer.add(button, cb.pos(1, i).weightX(1.0).anchor(GridBagConstraints.LINE_END).fill(GridBagConstraints.HORIZONTAL).build());
        }
        contentPane.add(inputContainer, BorderLayout.CENTER);

        Container buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, ScaleUtil.scale(4), ScaleUtil.scale(4)));
        JButton connectButton = new JButton(I18n.translate("menu.file.configure_keybinds.save"));
        connectButton.addActionListener(event -> save());
        buttonContainer.add(connectButton);
        JButton abortButton = new JButton(I18n.translate("prompt.cancel"));
        abortButton.addActionListener(event -> cancel());
        buttonContainer.add(abortButton);
        contentPane.add(buttonContainer, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private void save() {
        Set<KeyBind> modifiedKeyBinds = new HashSet<>();
        for (ConfigureCategoryKeyBindsDialog dialog : categoryDialogs) {
            if (dialog.isModified()) {
                modifiedKeyBinds.addAll(dialog.getModifiedKeyBinds());
            }
        }

        if (!modifiedKeyBinds.isEmpty()) {
            for (KeyBind keyBind : modifiedKeyBinds) {
                KeyBindsConfig.setKeyBind(keyBind);
            }
            KeyBindsConfig.save();
        }
        setVisible(false);
        dispose();
    }

    private void cancel() {
        setVisible(false);
        dispose();
    }

    public static void show(JFrame parent) {
        ConfigureKeyBindsDialog d = new ConfigureKeyBindsDialog(parent);

        d.setVisible(true);
    }
}

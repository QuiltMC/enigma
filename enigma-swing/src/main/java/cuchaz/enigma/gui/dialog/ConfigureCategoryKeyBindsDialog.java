package cuchaz.enigma.gui.dialog;

import cuchaz.enigma.gui.config.keybind.KeyBind;
import cuchaz.enigma.gui.config.keybind.KeyBinds;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class ConfigureCategoryKeyBindsDialog extends JDialog {
    private final List<KeyBindPanel> keyBindPanels = new ArrayList<>();
    private EditMode editMode = EditMode.ADD;
    private boolean modified;
    private List<KeyBind> modifiedKeyBinds;

    public ConfigureCategoryKeyBindsDialog(Frame owner, String category, List<KeyBind> keyBinds) {
        super(owner, I18n.translateFormatted("menu.file.configure_keybinds.category_title", I18n.translate("keybind.category." + category)), true);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        JLabel warningLabel = new JLabel(I18n.translate("menu.file.configure_keybinds.warning"));
        contentPane.add(warningLabel, BorderLayout.NORTH);
        JPanel inputContainer = new JPanel(new GridLayout(0, 1, 4, 4));
        inputContainer.setBorder(new EmptyBorder(ScaleUtil.scale(10), ScaleUtil.scale(10), ScaleUtil.scale(10), ScaleUtil.scale(10)));

        JButton editModeButton = new JButton(editMode.getTranslatedName());
        editModeButton.addActionListener(e -> {
            int index = editMode.index + 1;
            if (index >= EditMode.values().length) {
                index = 0;
            }
            editMode = EditMode.values()[index];
            editModeButton.setText(editMode.getTranslatedName());
            stopEditing(null);
        });
        inputContainer.add(editModeButton);

        for (KeyBind keyBind : keyBinds) {
            KeyBindPanel panel = new KeyBindPanel(keyBind);
            inputContainer.add(panel);
            keyBindPanels.add(panel);
        }

        contentPane.add(inputContainer, BorderLayout.CENTER);
        Container buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, ScaleUtil.scale(4), ScaleUtil.scale(4)));
        JButton saveButton = new JButton(I18n.translate("menu.file.configure_keybinds.save"));
        saveButton.addActionListener(e -> save());
        buttonContainer.add(saveButton);
        JButton cancelButton = new JButton(I18n.translate("prompt.cancel"));
        cancelButton.addActionListener(e -> cancel());
        buttonContainer.add(cancelButton);
        contentPane.add(buttonContainer, BorderLayout.SOUTH);
        pack();

        Dimension preferredSize = getPreferredSize();
        preferredSize.width = ScaleUtil.scale(400);
        setPreferredSize(preferredSize);
        pack();
        setLocationRelativeTo(owner);
    }

    private void save() {
        List<KeyBindPanel> modifiedPanels = keyBindPanels.stream().filter(KeyBindPanel::isModified).toList();
        if (!modifiedPanels.isEmpty()) {
            modified = true;
            modifiedKeyBinds = new ArrayList<>();
            for (KeyBindPanel panel : modifiedPanels) {
                modifiedKeyBinds.add(panel.getKeyBind());
            }
        }
        setVisible(false);
        dispose();
    }

    private void cancel() {
        setVisible(false);
        dispose();
    }

    private void stopEditing(KeyBindPanel panel) {
        for (KeyBindPanel keyBindPanel : keyBindPanels) {
            if (keyBindPanel != panel) {
                keyBindPanel.stopEditing();
            }
        }
    }

    protected boolean isModified() {
        return modified;
    }

    protected List<KeyBind> getModifiedKeyBinds() {
        return modifiedKeyBinds;
    }

    private enum EditMode {
        ADD("add", 0),
        REMOVE_FIRST("remove_first", 1),
        REMOVE_LAST("remove_last", 2),
        CLEAR("clear", 3),
        OVERWRITE("overwrite", 4);

        private final String key;
        private final int index;

        EditMode(String key, int index) {
            this.key = key;
            this.index = index;
        }

        public String getTranslatedName() {
            return I18n.translate("menu.file.configure_keybinds.edit_mode." + key);
        }
    }

    private class KeyBindPanel extends JPanel {
        private final Color defaultButtonFg;
        private final JButton button;
        private final KeyBind keyBind;
        private boolean editing = false;
        private boolean modified = false;

        private KeyBindPanel(KeyBind bind) {
            this.keyBind = bind.copy();

            setLayout(new BorderLayout());
            JLabel label = new JLabel(I18n.translate(bind.getTranslatedName()));
            add(label, BorderLayout.WEST);
            button = new JButton(getButtonText());
            defaultButtonFg = button.getForeground();
            button.addActionListener(e -> onButtonPressed());
            button.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (!handleKeyEvent(e)) {
                        super.keyPressed(e);
                    }
                }
            });
            add(button, BorderLayout.EAST);
        }

        private void onButtonPressed() {
            switch (editMode) {
                case REMOVE_FIRST:
                    stopEditing();
                    if (keyBind.hasKeyCodes()) {
                        keyBind.removeKeyCode(keyBind.getFirst());
                        update();
                    }
                    break;
                case REMOVE_LAST:
                    stopEditing();
                    if (keyBind.hasKeyCodes()) {
                        keyBind.removeKeyCode(keyBind.getLast());
                        update();
                    }
                    break;
                case CLEAR:
                    stopEditing();
                    if (keyBind.hasKeyCodes()) {
                        keyBind.clearKeyCodes();
                        update();
                    }
                    break;
                case OVERWRITE:
                case ADD:
                    if (editing) {
                        stopEditing();
                    } else {
                        startEditing();
                    }
                    break;
            }
        }

        private void update() {
            modified = true;
            button.setText(getButtonText());
        }

        private void startEditing() {
            editing = true;
            button.setForeground(Color.ORANGE);
            ConfigureCategoryKeyBindsDialog.this.stopEditing(this);
        }

        private void stopEditing() {
            editing = false;
            button.setForeground(defaultButtonFg);
        }

        private boolean handleKeyEvent(KeyEvent e) {
            if (editing) {
                if (KeyBinds.EXIT.matches(e)) {
                    stopEditing();
                } else if (editMode == EditMode.OVERWRITE) {
                    keyBind.clearKeyCodes();
                    keyBind.addKeyCode(e.getKeyCode());
                    update();
                    stopEditing();
                } else if (editMode == EditMode.ADD) {
                    keyBind.addKeyCode(e.getKeyCode());
                    button.setText(getButtonText());
                    update();
                } else {
                    stopEditing();
                }
                return true;
            } else {
                return false;
            }
        }

        public KeyBind getKeyBind() {
            return keyBind;
        }

        public boolean isModified() {
            return modified;
        }

        private String getButtonText() {
            return keyBind.getKeyCodes().stream()
                    .map(KeyEvent::getKeyText)
                    .reduce((a, b) -> a + ", " + b).orElse("<None>");
        }
    }
}

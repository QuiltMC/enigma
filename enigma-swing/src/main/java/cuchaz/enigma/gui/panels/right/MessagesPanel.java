package cuchaz.enigma.gui.panels.right;

import cuchaz.enigma.gui.Gui;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

public class MessagesPanel extends AbstractRightPanel {
    private final JPanel panel;
    private final JScrollPane messageScrollPane;
    private final JTextField chatBox;

    public MessagesPanel(Gui gui) {
        this.panel = new JPanel(new BorderLayout());
        this.messageScrollPane = new JScrollPane(gui.getMessages());
        this.chatBox = new JTextField();

        JPanel chatPanel = new JPanel(new BorderLayout());
        AbstractAction sendListener = new AbstractAction("Send") {
            @Override
            public void actionPerformed(ActionEvent e) {
                gui.sendMessage();
            }
        };
        this.chatBox.addActionListener(sendListener);
        JButton chatSendButton = new JButton(sendListener);
        chatPanel.add(this.chatBox, BorderLayout.CENTER);
        chatPanel.add(chatSendButton, BorderLayout.EAST);
        this.panel.add(this.messageScrollPane, BorderLayout.CENTER);
        this.panel.add(chatPanel, BorderLayout.SOUTH);
    }

    public JTextField getChatBox() {
        return this.chatBox;
    }

    public JScrollPane getMessageScrollPane() {
        return this.messageScrollPane;
    }

    @Override
    public ButtonPosition getButtonPosition() {
        return ButtonPosition.BOTTOM;
    }

    @Override
    public JPanel getPanel() {
        return this.panel;
    }

    @Override
    public String getId() {
        return "messages";
    }
}

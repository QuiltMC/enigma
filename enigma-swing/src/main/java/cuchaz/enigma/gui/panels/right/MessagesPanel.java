package cuchaz.enigma.gui.panels.right;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.network.packet.MessageC2SPacket;
import cuchaz.enigma.utils.I18n;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

public class MessagesPanel extends AbstractRightPanel {
    private final Gui gui;
    private final JPanel panel;
    private final JScrollPane messageScrollPane;
    private final JTextField pendingMessageBox;
    private final JButton sendPendingMessageButton;

    public MessagesPanel(Gui gui) {
        this.gui = gui;
        this.panel = new JPanel(new BorderLayout());
        this.messageScrollPane = new JScrollPane(gui.getMessages());
        this.pendingMessageBox = new JTextField();
        AbstractAction sendListener = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendPendingMessage();
            }
        };
        this.pendingMessageBox.addActionListener(sendListener);
        this.sendPendingMessageButton = new JButton(sendListener);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(this.pendingMessageBox, BorderLayout.CENTER);
        chatPanel.add(sendPendingMessageButton, BorderLayout.EAST);
        this.panel.add(this.messageScrollPane, BorderLayout.CENTER);
        this.panel.add(chatPanel, BorderLayout.SOUTH);
        // set button text
        this.retranslateUi();
    }

    private void sendPendingMessage() {
        // get message
        String text = this.pendingMessageBox.getText().trim();

        // send message, filtering out empty messages
        if (!text.isEmpty()) {
            this.gui.getController().sendPacket(new MessageC2SPacket(text));
        }

        // clear chat box
        this.pendingMessageBox.setText("");
    }

    public JScrollPane getMessageScrollPane() {
        return this.messageScrollPane;
    }

    @Override
    public void retranslateUi() {
        super.retranslateUi();
        this.sendPendingMessageButton.setText(I18n.translate("right_panel.messages.send"));
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

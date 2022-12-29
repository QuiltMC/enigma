package cuchaz.enigma.gui.panels.right;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.network.packet.MessageC2SPacket;

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

    public MessagesPanel(Gui gui) {
        this.gui = gui;
        this.panel = new JPanel(new BorderLayout());
        this.messageScrollPane = new JScrollPane(gui.getMessages());
        this.pendingMessageBox = new JTextField();

        JPanel chatPanel = new JPanel(new BorderLayout());
        AbstractAction sendListener = new AbstractAction("Send") {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendPendingMessage();
            }
        };
        this.pendingMessageBox.addActionListener(sendListener);
        JButton sendPendingMessageButton = new JButton(sendListener);
        chatPanel.add(this.pendingMessageBox, BorderLayout.CENTER);
        chatPanel.add(sendPendingMessageButton, BorderLayout.EAST);
        this.panel.add(this.messageScrollPane, BorderLayout.CENTER);
        this.panel.add(chatPanel, BorderLayout.SOUTH);
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

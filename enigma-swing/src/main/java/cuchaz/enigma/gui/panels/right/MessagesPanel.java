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

public class MessagesPanel extends MultiplayerOnlyRightPanel {
    private final Gui gui;
    private final JScrollPane messageScrollPane;
    private final JTextField pendingMessageBox;
    private final JButton sendPendingMessageButton;
    private final JPanel chatPanel;

    public MessagesPanel(Gui gui) {
        super(gui);
        this.gui = gui;
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
        this.chatPanel = new JPanel(new BorderLayout());
        this.chatPanel.add(this.pendingMessageBox, BorderLayout.CENTER);
        this.chatPanel.add(this.sendPendingMessageButton, BorderLayout.EAST);

        // set button text
        this.retranslateUi();

        // set online state
        this.setUp(!gui.isOffline());
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
    void addComponents() {
        this.add(this.messageScrollPane, BorderLayout.CENTER);
        this.add(chatPanel, BorderLayout.SOUTH);
    }

    @Override
    public ButtonPosition getButtonPosition() {
        return ButtonPosition.BOTTOM;
    }

    @Override
    public String getId() {
        return "messages";
    }
}

package cuchaz.enigma.gui.panels.right;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.network.packet.MessageC2SPacket;
import cuchaz.enigma.utils.I18n;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.function.Supplier;

public class OnlinePanel extends RightPanel {
    private final JLabel offlineLabel;
    private final JPanel whenOfflinePanel;
	private final JPanel whenOnlinePanel;
	private final JButton sendPendingMessageButton;
	private final JScrollPane messageScrollPane;
	private final JTextField pendingMessageBox;
	private final JLabel usersTitle;
	private final JLabel messagesTitle;
	private final JLabel titleCopy;

	private final Supplier<String> offlineTextProvider = () -> I18n.translate("right_panel.multiplayer.offline_text");
	private final Supplier<String> usersTitleProvider = () -> I18n.translate("right_panel.multiplayer.users_title");
	private final Supplier<String> messagesTitleProvider = () -> I18n.translate("right_panel.multiplayer.messages_title");
	private final Supplier<String> sendButtonTextProvider = () -> I18n.translate("right_panel.messages.send");

	private JPanel panel;
	private boolean offline;

    public OnlinePanel(Gui gui) {
        super(gui);

		// offline panel
        this.whenOfflinePanel = new JPanel(new BorderLayout());
        this.offlineLabel = new JLabel(this.offlineTextProvider.get());
		JPanel offlineTopPanel = new JPanel(new BorderLayout());

		// there are ghosts in my code
		this.titleCopy = new JLabel(this.titleProvider.get());

		offlineTopPanel.add(this.offlineLabel, BorderLayout.SOUTH);
        offlineTopPanel.add(this.titleCopy, BorderLayout.NORTH);
		this.whenOfflinePanel.add(offlineTopPanel, BorderLayout.NORTH);

		// online panel
		this.whenOnlinePanel = new JPanel(new BorderLayout());

		// top panel : user list
		JPanel topPanel = new JPanel(new BorderLayout());
		JPanel userListPanel = new JPanel(new BorderLayout());
		JScrollPane userScrollPane = new JScrollPane(gui.getUsers());

		this.usersTitle = new JLabel(this.usersTitleProvider.get());
		userListPanel.add(this.usersTitle, BorderLayout.NORTH);
		userListPanel.add(userScrollPane, BorderLayout.CENTER);

		topPanel.add(this.title, BorderLayout.NORTH);
		topPanel.add(userListPanel, BorderLayout.SOUTH);

		// bottom panel : messages
		JPanel bottomPanel = new JPanel(new BorderLayout());
		this.messageScrollPane = new JScrollPane(gui.getMessages());
		this.pendingMessageBox = new JTextField();
		AbstractAction sendListener = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendPendingMessage();
			}
		};
		this.pendingMessageBox.addActionListener(sendListener);
		this.sendPendingMessageButton = new JButton(this.sendButtonTextProvider.get());
		this.sendPendingMessageButton.setAction(sendListener);
		this.messagesTitle = new JLabel(this.messagesTitleProvider.get());
		JPanel chatPanel = new JPanel(new BorderLayout());
		chatPanel.add(this.pendingMessageBox, BorderLayout.CENTER);
		chatPanel.add(this.sendPendingMessageButton, BorderLayout.EAST);

		bottomPanel.add(this.messagesTitle, BorderLayout.NORTH);
		bottomPanel.add(this.messageScrollPane, BorderLayout.CENTER);
		bottomPanel.add(chatPanel, BorderLayout.SOUTH);

		this.whenOnlinePanel.add(topPanel, BorderLayout.NORTH);
		this.whenOnlinePanel.add(bottomPanel, BorderLayout.CENTER);

		this.setUp();
	}

	@Override
	public ButtonPosition getButtonPosition() {
		return ButtonPosition.BOTTOM;
	}

	@Override
	public String getId() {
		return "online";
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
        this.offlineLabel.setText(this.offlineTextProvider.get());
		this.sendPendingMessageButton.setText(this.sendButtonTextProvider.get());
		this.usersTitle.setText(this.usersTitleProvider.get());
		this.messagesTitle.setText(this.messagesTitleProvider.get());
		this.titleCopy.setText(this.titleProvider.get());
    }

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (this.gui.isOffline() != this.offline) {
			this.setUp();
		}
	}

    /**
     * sets up the panel for its offline or online state
     */
    public void setUp() {
		if (gui.isOffline() != this.offline) {
			this.offline = gui.isOffline();
			if (this.panel != null) {
				this.remove(this.panel);
			}

			if (this.offline) {
				this.panel = this.whenOfflinePanel;
			} else {
				this.panel = this.whenOnlinePanel;
			}

			this.add(this.panel);
		}
    }
}

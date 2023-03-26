package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.docker.component.DockerTitleBar;
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

public class CollabDocker extends Docker {
	private static final Supplier<String> OFFLINE_TEXT_PROVIDER = () -> I18n.translate("docker.collab.offline_text");
	private static final Supplier<String> START_SERVER_TEXT_PROVIDER = () -> I18n.translate("menu.collab.server.start");
	private static final Supplier<String> CONNECT_TO_SERVER_TEXT_PROVIDER = () -> I18n.translate("menu.collab.connect");

	private static final Supplier<String> USERS_TITLE_PROVIDER = () -> I18n.translate("docker.collab.users_title");
	private static final Supplier<String> MESSAGES_TITLE_PROVIDER = () -> I18n.translate("docker.collab.messages_title");
	private static final Supplier<String> SEND_BUTTON_TEXT_PROVIDER = () -> I18n.translate("docker.collab.send");

	private final JLabel offlineLabel;
	private final DockerTitleBar titleCopy;
	private final JButton startServerButton;
	private final JButton connectToServerButton;
	private final JPanel whenOfflinePanel;

	private final JPanel whenOnlinePanel;
	private final JButton sendPendingMessageButton;
	private final JScrollPane messageScrollPane;
	private final JTextField pendingMessageBox;
	private final JLabel usersTitle;
	private final JLabel messagesTitle;

	private JPanel panel;
	private boolean offline;

	public CollabDocker(Gui gui) {
		super(gui);

		// offline panel
		this.whenOfflinePanel = new JPanel(new BorderLayout());
		this.offlineLabel = new JLabel(OFFLINE_TEXT_PROVIDER.get());
		JPanel offlineTopPanel = new JPanel(new BorderLayout());

		JPanel connectionButtonPanel = new JPanel(new BorderLayout());
		this.startServerButton = new JButton(START_SERVER_TEXT_PROVIDER.get());
		this.connectToServerButton = new JButton(CONNECT_TO_SERVER_TEXT_PROVIDER.get());
		connectionButtonPanel.add(this.startServerButton, BorderLayout.NORTH);
		connectionButtonPanel.add(this.connectToServerButton, BorderLayout.SOUTH);

		this.startServerButton.addActionListener(e -> this.gui.getMenuBar().onStartServerClicked());
		this.connectToServerButton.addActionListener(e -> this.gui.getMenuBar().onConnectClicked());

		// we make a copy of the title bar to avoid having to shuffle it around both panels
		this.titleCopy = new DockerTitleBar(this, this.titleSupplier);

		offlineTopPanel.add(this.titleCopy, BorderLayout.NORTH);
		offlineTopPanel.add(this.offlineLabel, BorderLayout.CENTER);
		offlineTopPanel.add(connectionButtonPanel, BorderLayout.SOUTH);
		this.whenOfflinePanel.add(offlineTopPanel, BorderLayout.NORTH);

		// online panel
		this.whenOnlinePanel = new JPanel(new BorderLayout());

		// top panel : user list
		JPanel topPanel = new JPanel(new BorderLayout());
		JPanel userListPanel = new JPanel(new BorderLayout());
		JScrollPane userScrollPane = new JScrollPane(gui.getUsers());

		this.usersTitle = new JLabel(USERS_TITLE_PROVIDER.get());
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
				CollabDocker.this.sendPendingMessage();
			}
		};
		this.pendingMessageBox.addActionListener(sendListener);
		this.sendPendingMessageButton = new JButton(SEND_BUTTON_TEXT_PROVIDER.get());
		this.sendPendingMessageButton.setAction(sendListener);
		this.messagesTitle = new JLabel(MESSAGES_TITLE_PROVIDER.get());
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
	public Location getPreferredButtonLocation() {
		return new Location(Side.RIGHT, VerticalLocation.BOTTOM);
	}

	@Override
	public String getId() {
		return "collab";
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
		this.offlineLabel.setText(OFFLINE_TEXT_PROVIDER.get());
		this.sendPendingMessageButton.setText(SEND_BUTTON_TEXT_PROVIDER.get());
		this.usersTitle.setText(USERS_TITLE_PROVIDER.get());
		this.messagesTitle.setText(MESSAGES_TITLE_PROVIDER.get());
		this.startServerButton.setText(START_SERVER_TEXT_PROVIDER.get());
		this.connectToServerButton.setText(CONNECT_TO_SERVER_TEXT_PROVIDER.get());
		this.titleCopy.retranslateUi();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (this.gui.isOffline() != this.offline) {
			this.setUp();
		}
	}

	/**
	 * Sets up the panel for its offline or online state.
	 */
	public void setUp() {
		if (this.gui.isOffline() != this.offline) {
			this.offline = this.gui.isOffline();
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

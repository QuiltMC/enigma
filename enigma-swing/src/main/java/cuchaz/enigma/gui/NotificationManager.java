package cuchaz.enigma.gui;

import cuchaz.enigma.gui.docker.NotificationsDocker;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.ParameterizedMessage;
import cuchaz.enigma.utils.validation.ValidationContext;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A notifier that displays notifications in an {@link Gui}, and collects them so that their history can be stored in the {@link NotificationsDocker}.
 * <p>Can be used for notifications that go beyond the scale of {@link ValidationContext} errors, such as project opening notifications.
 * Notifications are held visible for a certain amount of time, and then removed from the {@link Gui}.
 * They can also be manually dismissed by the user.</p>
 */
public class NotificationManager implements ValidationContext.Notifier {
	public static final int REMOVE_CHECK_INTERVAL_MILLISECONDS = 5;
	public static final int TIMEOUT_MILLISECONDS = 10000;
	public static final int VERTICAL_GAP = 20;

	private final Gui gui;
	private JPanel glassPane;
	private final Map<Notification, Integer> activeNotifications = new HashMap<>();

	public NotificationManager(Gui gui) {
		this.gui = gui;

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				Set<Notification> keySetCopy = new HashSet<>(NotificationManager.this.activeNotifications.keySet());

				for (Notification notification : keySetCopy) {
					int timeout = NotificationManager.this.activeNotifications.get(notification);

					if (timeout <= 0) {
						NotificationManager.this.removeNotification(notification);
						continue;
					}

					int newTimeout = timeout - REMOVE_CHECK_INTERVAL_MILLISECONDS;
					NotificationManager.this.activeNotifications.put(notification, newTimeout);
					notification.getProgressBar().setValue(newTimeout);
				}
			}
		}, 0, REMOVE_CHECK_INTERVAL_MILLISECONDS);
	}

	private void removeNotification(Notification notification) {
		notification.setVisible(false);
		this.glassPane.remove(notification);
		this.activeNotifications.remove(notification);

		if (!this.activeNotifications.isEmpty()) {
			for (Notification activeNotification : this.activeNotifications.keySet()) {
				activeNotification.setLocation(activeNotification.getX(), this.getHeightFor(activeNotification));
			}
		}

		this.glassPane.revalidate();
	}

	private int getHeightFor(Notification notification) {
		// neatly orders notifications by their remaining time
		int height = this.glassPane.getHeight() - notification.getHeight() - VERTICAL_GAP;

		if (!this.activeNotifications.isEmpty()) {
			List<Notification> sortedNotifications = this.activeNotifications.keySet().stream().sorted((a, b) -> Integer.compare(this.activeNotifications.get(b), this.activeNotifications.get(a))).toList();
			int index = sortedNotifications.indexOf(notification);

			for (int i = index; i < sortedNotifications.size() - 1; i++) {
				height -= sortedNotifications.get(i + 1).getHeight() + VERTICAL_GAP;
			}
		}

		return height;
	}

	public void notify(ParameterizedMessage message) {
		Notification notificationPanel = new Notification(this.gui, message.getType(), message.getText(), message.getLongText(), true);

		JPanel glass = (JPanel) this.gui.getFrame().getGlassPane();
		this.glassPane = glass;

		// set up glass pane to actually display elements
		glass.setOpaque(false);
		glass.setVisible(true);
		glass.setLayout(null);
		glass.revalidate();

		glass.add(notificationPanel);

		// set up notification panel
		notificationPanel.revalidate();
		notificationPanel.setBounds(notificationPanel.getX(), notificationPanel.getY(), notificationPanel.getPreferredSize().width, notificationPanel.getPreferredSize().height);

		this.activeNotifications.put(notificationPanel, TIMEOUT_MILLISECONDS);
		this.gui.getDockerManager().getDocker(NotificationsDocker.class).addNotification(notificationPanel);

		notificationPanel.setLocation(glass.getWidth() - notificationPanel.getWidth() - notificationPanel.getWidth() / 8, this.getHeightFor(notificationPanel));
		notificationPanel.setVisible(true);
	}

	@Override
	public boolean verifyWarning(ParameterizedMessage message) {
		String text = message.getText() + (message.getLongText().length() > 0 ? "\n\n" + message.getLongText() : "") + " " + I18n.translate("notification.misc.continue");
		return JOptionPane.showConfirmDialog(this.gui.getFrame(), text, translateType(message.getType()), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
	}

	/**
	 * Gets all notifications that are currently active, meaning visible in the {@link Gui}.
	 * @return the active notifications, with their remaining time in milliseconds
	 */
	public Map<Notification, Integer> getActiveNotifications() {
		return this.activeNotifications;
	}

	private static String translateType(Message.Type type) {
		return I18n.translate("notification.type." + type.name().toLowerCase());
	}

	public enum ServerNotificationLevel {
		NONE,
		NO_CHAT,
		FULL;

		public String getText() {
			return I18n.translate("notification.level." + this.name().toLowerCase());
		}
	}

	/**
	 * Represents a notification that can be displayed in the {@link Gui}.
	 * Each notification has a unique id, so that the same message can be displayed multiple times without issue.
	 */
	public static class Notification extends JPanel {
		private final int id;
		private final Message.Type type;
		private final String title;
		private final String message;
		private final JProgressBar progressBar;

		public Notification(Gui gui, Message.Type type, String title, String message, boolean floating) {
			super(new BorderLayout());

			this.type = type;
			this.title = title;
			this.message = message;

			NotificationsDocker notificationsDocker = gui.getDockerManager().getDocker(NotificationsDocker.class);
			NotificationManager activeManager = gui.getNotificationManager();
			Set<Notification> notificationManagerNotifications = activeManager.getActiveNotifications().keySet();
			List<Notification> dockerNotifications = notificationsDocker.getNotifications();

			// set a unique id for this notification
			int newId = 0;
			while (true) {
				int finalNewId = newId;
				if (notificationManagerNotifications.stream().anyMatch(notification -> notification.id == finalNewId) || dockerNotifications.stream().anyMatch(notification -> notification.id == finalNewId)) {
					newId++;
				} else {
					break;
				}
			}

			this.id = newId;

			this.setBorder(BorderFactory.createLineBorder(Color.BLACK));

			JPanel messagePanel = new JPanel(new BorderLayout());
			if (message.isEmpty()) {
				messagePanel.add(new JLabel(title), BorderLayout.CENTER);
				messagePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
			} else {
				messagePanel.setBorder(BorderFactory.createTitledBorder(title));

				JTextArea text = new JTextArea(message);
				text.setOpaque(false);
				text.setEditable(false);
				text.setLineWrap(true);
				text.setWrapStyleWord(true);
				text.setFont(text.getFont().deriveFont(Font.BOLD));
				text.setMargin(new Insets(0, 0, 0, 0));
				text.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				text.setSize(Math.max(text.getPreferredSize().width, 300), text.getPreferredSize().height);
				messagePanel.add(text, BorderLayout.CENTER);
			}

			JPanel topBar = new JPanel(new BorderLayout());
			JButton dismissButton = new JButton("x");
			dismissButton.addActionListener(e -> {
				activeManager.removeNotification(this);
				if (!floating) {
					notificationsDocker.removeNotification(this);
				}
			});
			dismissButton.setMargin(new Insets(0, 4, 0, 4));

			topBar.add(dismissButton, BorderLayout.EAST);

			String whitespace = " ".repeat(title.length() > message.length() ? (int) ((title.length() - message.length()) * 2f) : 0);
			topBar.add(new JLabel(translateType(type) + "!" + whitespace), BorderLayout.WEST);
			topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));

			this.add(messagePanel, BorderLayout.CENTER);
			this.add(topBar, BorderLayout.NORTH);

			if (floating) {
				this.progressBar = new JProgressBar(0, TIMEOUT_MILLISECONDS);
				this.progressBar.setValue(TIMEOUT_MILLISECONDS);
				this.add(this.progressBar, BorderLayout.SOUTH);
			} else {
				this.progressBar = null;
			}
		}

		public Message.Type getType() {
			return this.type;
		}

		public String getTitle() {
			return this.title;
		}

		public String getMessage() {
			return this.message;
		}

		public JProgressBar getProgressBar() {
			return this.progressBar;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || this.getClass() != o.getClass()) return false;
			Notification that = (Notification) o;
			return this.id == that.id && Objects.equals(this.title, that.title) && Objects.equals(this.message, that.message);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.id, this.title, this.message);
		}
	}
}

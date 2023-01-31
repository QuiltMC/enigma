package cuchaz.enigma.gui;

import cuchaz.enigma.gui.docker.Docker;
import cuchaz.enigma.gui.docker.NotificationsDocker;
import cuchaz.enigma.utils.validation.ParameterizedMessage;
import cuchaz.enigma.utils.validation.ValidationContext;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class NotificationManager implements ValidationContext.Notifier {
	public static final int REMOVE_CHECK_INTERVAL_MILLISECONDS = 5;
	public static final int TIMEOUT_MILLISECONDS = 10000;
	public static final int VERTICAL_GAP = 20;

	private final Gui gui;
	private JPanel glassPane;
	private final Map<Notification, Integer> activeNotifications = new HashMap<>();

	public NotificationManager(Gui gui) {
		// todo more notifications:
		// server start, join, stop

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

			for (int i = index; i < sortedNotifications.size() - 1; i ++) {
				height -= sortedNotifications.get(i).getHeight() + VERTICAL_GAP;
			}
		}

		return height;
	}

	public void notify(ParameterizedMessage message) {
		Notification notificationPanel = new Notification(this.gui, message.getText(), message.getLongText(), true);

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
		notificationPanel.setSize(300, 100);

		this.activeNotifications.put(notificationPanel, TIMEOUT_MILLISECONDS);
		Docker.getDocker(NotificationsDocker.class).addNotification(notificationPanel);

		notificationPanel.setLocation(glass.getWidth() - notificationPanel.getWidth() - notificationPanel.getWidth() / 8, this.getHeightFor(notificationPanel));
		notificationPanel.setVisible(true);
	}

	public Map<Notification, Integer> getActiveNotifications() {
		return this.activeNotifications;
	}

	public static class Notification extends JPanel {
		private final int id;
		private final String title;
		private final String message;
		private final JProgressBar progressBar;

		public Notification(Gui gui, String title, String message, boolean floating) {
			super(new BorderLayout());

			this.title = title;
			this.message = message;

			NotificationsDocker notificationsDocker = Docker.getDocker(NotificationsDocker.class);
			NotificationManager activeManager = gui.getNotificationManager();
			Set<Notification> notificationManagerNotifications = activeManager.getActiveNotifications().keySet();
			List<Notification> dockerNotifications = notificationsDocker.getNotifications();

			// set a unique id for this notification
			int newId = 0;
			while (true) {
				int finalNewId = newId;
				if (notificationManagerNotifications.stream().anyMatch(notification -> notification.id == finalNewId) || dockerNotifications.stream().anyMatch(notification -> notification.id == finalNewId)) {
					newId ++;
				} else {
					break;
				}
			}

			this.id = newId;

			this.setBorder(BorderFactory.createLineBorder(Color.BLACK));

			JPanel messagePanel = new JPanel(new BorderLayout());
			messagePanel.setBorder(BorderFactory.createTitledBorder(title));
			messagePanel.add(new JLabel(message), BorderLayout.CENTER);

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
			topBar.add(new JLabel(this.id + ""), BorderLayout.WEST);
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

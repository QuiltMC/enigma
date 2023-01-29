package cuchaz.enigma.gui;

import cuchaz.enigma.utils.validation.ValidationContext;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class NotificationManager implements ValidationContext.Notifier {
	private static final int REMOVE_CHECK_INTERVAL_MILLISECONDS = 1000;
	private static final int TIMEOUT_MILLISECONDS = 10 * REMOVE_CHECK_INTERVAL_MILLISECONDS;

	private final Gui gui;
	private JPanel glassPane;
	private final Map<JPanel, Integer> activeNotifications = new HashMap<>();

	public NotificationManager(Gui gui) {
		this.gui = gui;

		// a task, running every 1ms, that checks if any notifications have timed out
		int delayMilliseconds = 1;
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				Set<JPanel> keySetCopy = new HashSet<>(NotificationManager.this.activeNotifications.keySet());

				for (JPanel notification : keySetCopy) {
					int timeout = NotificationManager.this.activeNotifications.get(notification);

					if (timeout <= 0) {
						NotificationManager.this.removeNotification(notification);
						continue;
					}

					NotificationManager.this.activeNotifications.put(notification, timeout - delayMilliseconds);
				}
			}
		}, 0, delayMilliseconds);
	}

	private void removeNotification(JPanel notification) {
		notification.setVisible(false);
		this.glassPane.remove(notification);
		this.activeNotifications.remove(notification);

		if (!this.activeNotifications.isEmpty()) {
			for (JPanel activeNotification : this.activeNotifications.keySet()) {
				activeNotification.setLocation(activeNotification.getX(), this.getHeightFor(notification));
			}
		}

		this.glassPane.revalidate();
	}

	private int getHeightFor(JPanel notification) {
		int height = this.glassPane.getHeight() - notification.getHeight() - notification.getHeight() / 4;
		for (JPanel panel : this.activeNotifications.keySet()) {
			if (!panel.equals(notification)) {
				height -= panel.getHeight() + panel.getHeight() / 4;
			}
		}

		return height;
	}

	private JPanel createNotificationPanel(String title, String message) {
		JPanel notificationPanel = new JPanel(new BorderLayout());
		notificationPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

		JPanel messagePanel = new JPanel(new BorderLayout());
		messagePanel.setBorder(BorderFactory.createTitledBorder(title));
		messagePanel.add(new JLabel(message), BorderLayout.CENTER);

		JPanel topBar = new JPanel(new BorderLayout());
		JButton dismissButton = new JButton("x");
		dismissButton.addActionListener(e -> this.removeNotification(notificationPanel));
		dismissButton.setMargin(new Insets(0, 4, 0, 4));

		topBar.add(dismissButton, BorderLayout.EAST);
		topBar.add(new JLabel("error!"), BorderLayout.WEST);
		topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));

		notificationPanel.add(messagePanel, BorderLayout.CENTER);
		notificationPanel.add(topBar, BorderLayout.NORTH);

		return notificationPanel;
	}

	public void notify(String title, String message) {
		JPanel notificationPanel = this.createNotificationPanel(title, message);

		JPanel glass = (JPanel) this.gui.getFrame().getGlassPane();
		this.glassPane = glass;

		// set up glass pane to actually display elements
		glass.setOpaque(false);
		glass.setVisible(true);
		glass.setLayout(null);
		glass.revalidate();

		glass.add(notificationPanel);

		// set up notification panel
		notificationPanel.setVisible(true);
		notificationPanel.revalidate();
		notificationPanel.setSize(300, 100);

		notificationPanel.setLocation(glass.getWidth() - notificationPanel.getWidth() - notificationPanel.getWidth() / 8, this.getHeightFor(notificationPanel));

		this.activeNotifications.put(notificationPanel, TIMEOUT_MILLISECONDS);
	}
}

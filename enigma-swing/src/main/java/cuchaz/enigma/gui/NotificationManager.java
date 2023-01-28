package cuchaz.enigma.gui;

import cuchaz.enigma.utils.validation.ValidationContext;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class NotificationManager implements ValidationContext.Notifier {
	private final Gui gui;
	private JPanel glassPane;
	private final Map<JPanel, Integer> activeNotifications = new HashMap<>();

	public NotificationManager(Gui gui) {
		this.gui = gui;

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				Set<JPanel> keySetCopy = new HashSet<>(NotificationManager.this.activeNotifications.keySet());

				for (JPanel notification : keySetCopy) {
					int timeout = NotificationManager.this.activeNotifications.get(notification);

					if (timeout <= 0) {
						notification.setVisible(false);
						NotificationManager.this.glassPane.remove(notification);
						NotificationManager.this.activeNotifications.remove(notification);
						continue;
					}

					NotificationManager.this.activeNotifications.put(notification, timeout - 1);
				}
			}
		}, 1, 1);
	}

	public void notify(String title, String message) {
		JPanel notificationPanel = new JPanel();
		notificationPanel.add(new JLabel(title));
		notificationPanel.setBackground(Color.PINK);

		JPanel glass = (JPanel) this.gui.getFrame().getGlassPane();
		glass.add(notificationPanel);
		this.glassPane = glass;
		notificationPanel.setLocation(glass.getWidth() / 2, glass.getHeight() / 2);

		// set up glass pane to actually display elements
		glass.setOpaque(false);
		glass.setVisible(true);

		// set up notification panel
		notificationPanel.setVisible(true);
		notificationPanel.revalidate();
		notificationPanel.setSize(300, 300);

		this.activeNotifications.put(notificationPanel, 5000);
	}
}

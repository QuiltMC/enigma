package cuchaz.enigma.gui.docker;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.NotificationManager;
import cuchaz.enigma.gui.docker.component.VerticalFlowLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

public class NotificationsDocker extends Docker {
	private final JPanel notificationPanel;

	public NotificationsDocker(Gui gui) {
		super(gui);

		this.notificationPanel = new JPanel(new VerticalFlowLayout(NotificationManager.VERTICAL_GAP));

		JScrollPane scrollPane = new JScrollPane(this.notificationPanel);
		this.add(scrollPane);
	}

	public void addNotification(NotificationManager.Notification notification) {
		if (this.gui.getNotificationManager().getActiveNotifications().isEmpty()) {
			return;
		}

		List<NotificationManager.Notification> notifications = this.getNotifications();

		if (!notifications.contains(notification)) {
			this.notificationPanel.add(new NotificationManager.Notification(this.gui, notification.getType(), notification.getTitle(), notification.getMessage(), false));
		}
	}

	public List<NotificationManager.Notification> getNotifications() {
		List<NotificationManager.Notification> notifications = new ArrayList<>();
		for (Component component : this.notificationPanel.getComponents()) {
			if (component instanceof NotificationManager.Notification notification) {
				notifications.add(notification);
			}
		}

		return notifications;
	}

	public void removeNotification(NotificationManager.Notification notification) {
		this.notificationPanel.remove(notification);
	}

	@Override
	public String getId() {
		return "notifications";
	}

	@Override
	public Location getButtonPosition() {
		return new Location(Side.RIGHT, VerticalLocation.BOTTOM);
	}

	@Override
	public Location getPreferredLocation() {
		return new Location(Side.RIGHT, VerticalLocation.BOTTOM);
	}
}

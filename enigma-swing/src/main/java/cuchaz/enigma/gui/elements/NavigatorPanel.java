package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A panel with buttons to navigate to the next and previous items in its entry collection.
 */
public class NavigatorPanel extends JPanel {
	private final Gui gui;
	private final JLabel statsLabel;
	private final List<Entry<?>> entries = new ArrayList<>();
	private final Function<Entry<?>, Boolean> validityChecker;

	private int currentIndex = 0;

	/**
	 * Creates a new navigator panel.
	 * @param gui the parent gui
	 * @param validityChecker a function that check if an entry should still be accessible from this panel. returns {@code true} if yes, or {@code false} if it should be removed
	 */
	public NavigatorPanel(Gui gui, Function<Entry<?>, Boolean> validityChecker) {
		super();
		this.gui = gui;
		this.validityChecker = validityChecker;
		this.statsLabel = new JLabel("0/0");

		JButton up = new JButton("⋀");
		up.addActionListener(event -> {
			if (!this.entries.isEmpty()) {
				this.currentIndex--;
				if (this.currentIndex < 0) {
					this.currentIndex = this.entries.size() - 1;
				}

				this.tryNavigate();
			}
		});

		JButton down = new JButton("⋁");
		down.addActionListener(event -> {
			if (!this.entries.isEmpty()) {
				this.currentIndex++;
				if (this.currentIndex >= this.entries.size()) {
					this.currentIndex = 0;
				}

				this.tryNavigate();
			}
		});

		this.add(up);
		this.add(down);
		this.add(this.statsLabel);
		this.setBorder(new LineBorder(Color.BLACK));
	}

	private void tryNavigate() {
		this.checkForRemoval(this.entries.get(this.currentIndex));
		this.updateStatsLabel();
		this.gui.getController().navigateTo(this.entries.get(this.currentIndex));
	}

	/**
	 * Adds the entry if it's valid for this navigator.
	 * @param entry the entry to add
	 */
	public void tryAddEntry(@Nullable Entry<?> entry) {
		if (entry != null && !this.entries.contains(entry) && this.validityChecker.apply(entry)) {
			this.entries.add(entry);
			this.statsLabel.setText((this.currentIndex + 1) + "/" + this.entries.size());
		}
	}

	/**
	 * Checks if the entry should be removed, and if so handles removal and updates.
	 * @param target the entry to check
	 */
	public void checkForRemoval(Entry<?> target) {
		if (!this.validityChecker.apply(target)) {
			this.entries.remove(target);
			this.updateStatsLabel();
		}
	}

	private void updateStatsLabel() {
		this.statsLabel.setText((this.currentIndex + 1) + "/" + this.entries.size());
	}
}

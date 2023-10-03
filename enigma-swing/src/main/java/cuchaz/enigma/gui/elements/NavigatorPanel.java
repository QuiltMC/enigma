package cuchaz.enigma.gui.elements;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.source.RenamableTokenType;
import cuchaz.enigma.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A panel with buttons to navigate to the next and previous items in its entry collection.
 */
public class NavigatorPanel extends JPanel {
	private static final RenamableTokenType[] SUPPORTED_TOKEN_TYPES = {RenamableTokenType.OBFUSCATED, RenamableTokenType.PROPOSED, RenamableTokenType.DEOBFUSCATED};

	private final Gui gui;
	private final JLabel statsLabel;
	private final Map<RenamableTokenType, List<Entry<?>>> entries = new HashMap<>();
	private final Map<Entry<?>, RenamableTokenType> tokenTypes = new HashMap<>();

	private int currentIndex = 0;
	private RenamableTokenType selectedType;

	/**
	 * Creates a new navigator panel.
	 * @param gui the parent gui
	 */
	public NavigatorPanel(Gui gui) {
		super();
		this.gui = gui;
		this.statsLabel = new JLabel("0/0");

		JComboBox<RenamableTokenType> typeSelector = new JComboBox<>(SUPPORTED_TOKEN_TYPES);
		typeSelector.addItemListener(event -> {
			if (event.getStateChange() == ItemEvent.SELECTED) {
				this.selectedType = (RenamableTokenType) event.getItem();
				this.onTypeChange();
			}
		});
		this.selectedType = RenamableTokenType.OBFUSCATED;

		for (RenamableTokenType type : SUPPORTED_TOKEN_TYPES) {
			this.entries.put(type, new ArrayList<>());
		}

		JButton up = new JButton(GuiUtil.getUpChevron());
		up.addActionListener(event -> this.navigateUp());
		JButton down = new JButton(GuiUtil.getDownChevron());
		down.addActionListener(event -> this.navigateDown());

		this.add(typeSelector);
		this.add(up);
		this.add(down);
		this.add(this.statsLabel);

		// transparent background
		this.setBackground(new Color(0, 0, 0, 0));
	}

	/**
	 * Navigates to the next entry matching the current filter.
	 */
	public void navigateDown() {
		List<Entry<?>> currentEntrySet = this.entries.get(this.selectedType);
		if (!currentEntrySet.isEmpty()) {
			this.currentIndex++;
			this.wrapIndex();

			this.tryNavigate();
		}
	}

	/**
	 * Navigates to the last entry matching the current filter.
	 */
	public void navigateUp() {
		List<Entry<?>> currentEntrySet = this.entries.get(this.selectedType);
		if (!currentEntrySet.isEmpty()) {
			this.currentIndex--;
			this.wrapIndex();

			this.tryNavigate();
		}
	}

	private void onTypeChange() {
		this.wrapIndex();
		this.updateStatsLabel();
	}

	private void wrapIndex() {
		List<Entry<?>> currentEntrySet = this.entries.get(this.selectedType);
		if (this.currentIndex < 0) {
			this.currentIndex = currentEntrySet.size() - 1;
		} else if (this.currentIndex >= currentEntrySet.size()) {
			this.currentIndex = 0;
		}
	}

	private void tryNavigate() {
		this.updateTokenType(this.entries.get(this.selectedType).get(this.currentIndex));
		this.updateStatsLabel();
		this.gui.getController().navigateTo(this.entries.get(this.selectedType).get(this.currentIndex));
	}

	/**
	 * Removes all data from this navigator and updates its UI.
	 * Keeps selected type intact.
	 */
	public void clear() {
		this.tokenTypes.clear();
		for (var list : this.entries.values()) {
			list.clear();
		}

		this.currentIndex = 0;
	}

	/**
	 * Adds the provided entry to this navigator's pool and sorts it.
	 * @param entry the entry to add
	 */
	public void addEntry(@Nullable Entry<?> entry) {
		EnigmaProject project = this.gui.getController().getProject();
		if (entry != null && project.isRenamable(entry) && project.isNavigable(entry)) {
			RenamableTokenType tokenType = this.getTokenType(entry);
			List<Entry<?>> entries = this.entries.get(tokenType);

			if (!entries.contains(entry)) {
				entries.add(entry);
				this.tokenTypes.put(entry, tokenType);
				this.updateStatsLabel();
			}
		}
	}

	/**
	 * Checks if the entry should be moved to a different token type, and updates it if so.
	 * Assumes that the entry's old token type matches the currently selected token type.
	 * @param target the entry to check
	 */
	public void updateTokenType(Entry<?> target) {
		RenamableTokenType tokenType = this.getTokenType(target);
		RenamableTokenType oldType = this.tokenTypes.get(target);

		if (tokenType != oldType) {
			this.entries.get(oldType).remove(target);
			this.entries.get(tokenType).add(target);
			this.tokenTypes.put(target, tokenType);
			this.updateStatsLabel();
		}
	}

	private RenamableTokenType getTokenType(Entry<?> target) {
		EnigmaProject project = this.gui.getController().getProject();
		RenamableTokenType tokenType = project.getMapper().extendedDeobfuscate(target).getType();
		if (tokenType == RenamableTokenType.OBFUSCATED) {
			if (project.hasProposedName(target)) {
				tokenType = RenamableTokenType.PROPOSED;
			}
		}

		return tokenType;
	}

	private void updateStatsLabel() {
		int index = this.entries.get(this.selectedType).isEmpty() ? 0 : this.currentIndex + 1;
		String indexString = String.valueOf(index).length() == 1 ? "0" + index : String.valueOf(index);
		this.statsLabel.setText(indexString + "/" + this.entries.get(this.selectedType).size());
	}
}

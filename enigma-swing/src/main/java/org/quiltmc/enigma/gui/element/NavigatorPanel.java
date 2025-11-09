package org.quiltmc.enigma.gui.element;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.gui.EditableType;
import org.quiltmc.enigma.gui.Gui;
import org.quiltmc.enigma.gui.util.GuiUtil;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A panel with buttons to navigate to the next and previous items in its entry collection.
 */
public class NavigatorPanel extends JPanel {
	private static final TokenType[] SUPPORTED_TOKEN_TYPES = {TokenType.OBFUSCATED, TokenType.JAR_PROPOSED, TokenType.DYNAMIC_PROPOSED, TokenType.DEOBFUSCATED};

	private final Gui gui;
	private final JLabel statsLabel;
	private final Set<Entry<?>> allEntries = new LinkedHashSet<>();
	private final Map<TokenType, Map<Entry<?>, Integer>> entryIndexesByType = new HashMap<>();

	private int currentIndex = 0;
	private TokenType selectedType;

	/**
	 * Creates a new navigator panel.
	 * @param gui the parent gui
	 */
	public NavigatorPanel(Gui gui) {
		super();
		this.gui = gui;
		this.statsLabel = new JLabel("0/0");

		JComboBox<TokenType> typeSelector = new JComboBox<>(SUPPORTED_TOKEN_TYPES);
		typeSelector.addItemListener(event -> {
			if (event.getStateChange() == ItemEvent.SELECTED) {
				this.selectedType = (TokenType) event.getItem();
				this.onTypeChange();
			}
		});
		this.selectedType = TokenType.OBFUSCATED;

		this.initEntryIndexesByType();

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

	private void initEntryIndexesByType() {
		for (TokenType type : SUPPORTED_TOKEN_TYPES) {
			this.entryIndexesByType.put(type, new LinkedHashMap<>());
		}
	}

	/**
	 * Navigates to the next entry matching the current filter.
	 */
	public void navigateDown() {
		this.tryNavigate(false);
	}

	/**
	 * Navigates to the last entry matching the current filter.
	 */
	public void navigateUp() {
		this.tryNavigate(true);
	}

	private void onTypeChange() {
		this.wrapIndex();
		this.updateStatsLabel();
	}

	private void wrapIndex() {
		final int selectedEntryTypeCount = this.entryIndexesByType.get(this.selectedType).size();
		if (this.currentIndex < 0) {
			this.currentIndex = selectedEntryTypeCount - 1;
		} else if (this.currentIndex >= selectedEntryTypeCount) {
			this.currentIndex = 0;
		}
	}

	public void decrementIndex() {
		this.currentIndex = Math.max(this.currentIndex - 1, 0);
	}

	private void tryNavigate(boolean reverse) {
		Map<Entry<?>, Integer> selectedEntryIndexes = this.entryIndexesByType.get(this.selectedType);
		if (!selectedEntryIndexes.isEmpty()) {
			Entry<?> entry = this.getClosestEntryToCursor(selectedEntryIndexes.keySet(), reverse);
			this.gui.getController().navigateTo(entry);
			this.currentIndex = selectedEntryIndexes.get(entry);
			this.updateStatsLabel();
		}
	}

	public Entry<?> getClosestEntryToCursor(Collection<Entry<?>> currentEntrySet, boolean reverse) {
		List<Entry<?>> possibleEntriesCopy = new ArrayList<>(currentEntrySet);
		if (reverse) {
			Collections.reverse(possibleEntriesCopy);
		}

		int cursorPos = this.gui.getActiveEditor().getEditor().getCaretPosition();
		for (Entry<?> entry : possibleEntriesCopy) {
			List<Token> tokens = this.gui.getController().getTokensForReference(this.gui.getActiveEditor().getSource(), EntryReference.declaration(entry, entry.getName()));
			if (!tokens.isEmpty()) {
				Token token = tokens.get(0);
				if (reverse ? token.start < cursorPos : token.start > cursorPos) {
					return entry;
				}
			}
		}

		return possibleEntriesCopy.get(0);
	}

	public void resetEntries(Iterable<Entry<?>> newEntries) {
		this.allEntries.clear();
		this.initEntryIndexesByType();

		EnigmaProject project = this.gui.getController().getProject();
		for (Entry<?> entry : newEntries) {
			if (entry != null && this.gui.isEditable(EditableType.fromEntry(entry)) && project.isRenamable(entry) && project.isNavigable(entry)) {
				if (!this.allEntries.contains(entry)) {
					Map<Entry<?>, Integer> entryIndexesOfType = this.entryIndexesByType.get(this.getTokenType(entry));
					if (entryIndexesOfType != null) {
						this.allEntries.add(entry);
						entryIndexesOfType.put(entry, entryIndexesOfType.size());
					}
				}
			}
		}

		this.updateStatsLabel();
	}

	/**
	 * Rechecks and updates all token types.
	 */
	public void updateAllTokenTypes() {
		this.initEntryIndexesByType();

		for (Entry<?> entry : this.allEntries) {
			Map<Entry<?>, Integer> entryIndexesOfType = this.entryIndexesByType.get(this.getTokenType(entry));
			if (entryIndexesOfType != null) {
				entryIndexesOfType.put(entry, entryIndexesOfType.size());
			}
		}
	}

	private TokenType getTokenType(Entry<?> target) {
		// make sure we're checking from the root of the inheritance tree
		EnigmaProject project = this.gui.getController().getProject();
		Entry<?> rootEntry = project.getRemapper().getObfResolver().resolveFirstEntry(target, ResolutionStrategy.RESOLVE_ROOT);

		return project.getRemapper().getMapping(rootEntry).tokenType();
	}

	private void updateStatsLabel() {
		final Map<Entry<?>, Integer> entryIndexesOfType = this.entryIndexesByType.get(this.selectedType);
		final String index = String.valueOf(entryIndexesOfType.isEmpty() ? 0 : this.currentIndex + 1);
		final String total = String.valueOf(entryIndexesOfType.size());

		final int lengthDiff = total.length() - index.length();
		final String paddedIndex = lengthDiff == 0 ? index : "0".repeat(lengthDiff) + index;

		this.statsLabel.setText(paddedIndex + "/" + total);
	}
}

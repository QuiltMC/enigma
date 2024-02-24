package org.quiltmc.enigma.gui.search;

import java.util.List;

public interface SearchEntry {
	List<String> getSearchableNames();

	/**
	 * Returns a type that uniquely identifies this search entry across possible changes.
	 * This is used for tracking the amount of times this entry has been selected.
	 *
	 * @return a unique identifier for this search entry
	 */
	String getIdentifier();

	/**
	 * Returns the priority of the type of this entry. Entries with higher
	 * priorities show up higher in the result list. Should be greater than zero.
	 */
	int getTypePriority();
}

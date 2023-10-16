package org.quiltmc.enigma.gui.config;

import org.quiltmc.enigma.util.Pair;
import org.tinylog.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecentFiles {
	/**
	 * Adds a new file pair first in the recent files list, limiting the new list's size to {@link #MAX_RECENT_FILES}. If the pair is already in the list, moves it to the top.
	 * @param jar a path to the jar being mapped
	 * @param mappings a path to the mappings save location
	 */
	public static void addRecentFilePair(Path jar, Path mappings) {
		var pairs = getRecentFilePairs();
		var pair = new Pair<>(jar, mappings);

		pairs.remove(pair);
		pairs.add(0, pair);

		ui.data().setArray(RECENT_FILES, pairs.stream().limit(getMaxRecentFiles()).map(p -> p.a().toString() + PAIR_SEPARATOR + p.b().toString()).toArray(String[]::new));
	}

	/**
	 * Returns the most recently accessed project.
	 * @return A pair containing the jar path as its left element and the mappings path as its right element.
	 */
	public static Optional<Pair<Path, Path>> getMostRecentFilePair() {
		var recentFilePairs = getRecentFilePairs();
		if (recentFilePairs.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(recentFilePairs.get(0));
	}

	/**
	 * Returns all recently accessed projects, up to a limit of {@link #MAX_RECENT_FILES}.
	 * @return a list of pairs containing the jar path as their left element and the mappings path as their right element.
	 */
	public static List<Pair<Path, Path>> getRecentFilePairs() {
		List<Pair<Path, Path>> pairs = new ArrayList<>();

		String[] pairsArray = ui.data().getArray(RECENT_FILES).orElse(new String[0]);

		for (String filePair : pairsArray) {
			if (!filePair.isBlank()) {
				var pairOptional = parseFilePair(filePair);

				if (pairOptional.isPresent()) {
					pairs.add(pairOptional.get());
				} else {
					Logger.error("failed to read recent file state for {}, ignoring!", filePair);
				}
			}
		}

		return pairs;
	}

	private static Optional<Pair<Path, Path>> parseFilePair(String pair) {
		String[] split = pair.split(PAIR_SEPARATOR);

		if (split.length != 2) {
			return Optional.empty();
		}

		String jar = split[0];
		String mappings = split[1];
		return Optional.of(new Pair<>(Paths.get(jar), Paths.get(mappings)));
	}
}

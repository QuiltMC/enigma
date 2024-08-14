package quilt.internal.task;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class PrintColorKeyGroupsMapCodeTask extends DefaultTask {
	@TaskAction
	public void run() {
		printDistinctColorKeyGroupsMapCode(
			UIManager.getLookAndFeel(),
			new MetalLookAndFeel(),
			new FlatLightLaf(),
			new FlatDarkLaf(),
			new FlatDarculaLaf()
		);
	}

	private static List<Set<String>> getDistinctColorKeyGroups(LookAndFeel... lookAndFeels) {
		final List<List<Set<String>>> themesKeyGroups = Arrays.stream(lookAndFeels)
			.map(PrintColorKeyGroupsMapCodeTask::getUniqueColors)
			.map(uniqueColors ->
				uniqueColors.values()
					.stream()
					.map(keyObjects ->
						keyObjects.stream()
							.map(Object::toString)
							.collect(Collectors.<String, Set<String>>toCollection(LinkedHashSet::new))
					)
					.toList()
			).toList();

		// map from each color key to each color key that always shares the same color
		final Map<String, Set<String>> keysToDistinctGroups = new LinkedHashMap<>();

		themesKeyGroups.forEach(themeKeyGroups ->
			themeKeyGroups.forEach(themeKeyGroup ->
				themeKeyGroup.stream().filter(keysToDistinctGroups.keySet()::contains).findAny().ifPresentOrElse(
					matchingKey -> {
						final Set<String> distinctKeyGroup = keysToDistinctGroups.get(matchingKey);

						if (!distinctKeyGroup.containsAll(themeKeyGroup)) {
							if (themeKeyGroup.containsAll(distinctKeyGroup)) {
								themeKeyGroup.forEach(key -> {
									final Set<String> groupToShrink = keysToDistinctGroups.get(key);
									if (groupToShrink != null && groupToShrink.size() > 1) {
										groupToShrink.remove(key);
									}

									keysToDistinctGroups.put(key, themeKeyGroup);
								});
							} else {
								// neither group contains the other: split groups

								final Set<String> commonKeyGroup = new LinkedHashSet<>(distinctKeyGroup);
								commonKeyGroup.retainAll(themeKeyGroup);

								final Set<String> distinctKeyGroupUniques = new LinkedHashSet<>(distinctKeyGroup);
								distinctKeyGroupUniques.removeAll(commonKeyGroup);

								final Set<String> themeKeyGroupUniques = new LinkedHashSet<>(themeKeyGroup);
								themeKeyGroupUniques.removeAll(commonKeyGroup);

								themeKeyGroupUniques.forEach(uniqueThemGroupKey -> {
									@Nullable
									final Set<String> groupToShrink = keysToDistinctGroups.get(uniqueThemGroupKey);

									if (groupToShrink != null && groupToShrink.size() > 1) {
										groupToShrink.remove(uniqueThemGroupKey);
									}
								});

								Stream.of(
										commonKeyGroup,
										distinctKeyGroupUniques,
										themeKeyGroupUniques
									)
									.forEach(newKeyGroup ->
										newKeyGroup.forEach(groupedKey ->
											keysToDistinctGroups.put(groupedKey, newKeyGroup)
										)
									);
							}
						}
					},
					() -> themeKeyGroup.forEach(newGroupedKey ->
						keysToDistinctGroups.put(newGroupedKey, themeKeyGroup)
					)
				)
			)
		);

		final List<Set<String>> distinctGroups = new LinkedList<>();

		while (!keysToDistinctGroups.isEmpty()) {
			final Set<String> distinctGroup = keysToDistinctGroups.values().iterator().next();

			distinctGroups.add(distinctGroup);

			distinctGroup.forEach(keysToDistinctGroups::remove);
		}

		return distinctGroups;
	}

	private static void printDistinctColorKeyGroupsMapCode(LookAndFeel... lookAndFeels) {
		final List<Set<String>> distinctGroups = getDistinctColorKeyGroups(lookAndFeels);

		final int[] i = {0};
		System.out.println(
			"Map.<String, List<String>>ofEntries(\n" +

				distinctGroups.stream()
					.flatMap(group ->
						Stream.of(
							"Map.entry(\"group-" + i[0]++ + "\", List.of(\n" +

								group.stream()
									.map(key -> "\"" + key + "\"")
									.collect(Collectors.joining(",\n")) +

								"\n))"
						)
					).collect(Collectors.joining(",\n")) +

				"\n);"
		);
	}

	private static Map<Color, List<Object>> getUniqueColors(LookAndFeel lookAndFeel) {
		final var colors = lookAndFeel.getDefaults()
			.entrySet()
			.stream()
			.map(entry -> entry.getValue() instanceof Color color ?
				Optional.of(Map.entry(entry.getKey(), color)) :
				Optional.<Map.Entry<Object, Color>>empty()
			)
			.flatMap(Optional::stream)
			.toList();

		final var uniqueColors = new HashMap<Color, List<Object>>();
		colors.forEach(entry -> {
			final Color color = entry.getValue();
			if (uniqueColors.containsKey(color)) {
				uniqueColors.get(color).add(entry.getKey());
			} else {
				final var keys = new ArrayList<>();
				keys.add(entry.getKey());
				uniqueColors.put(color, keys);
			}
		});

		return uniqueColors;
	}
}

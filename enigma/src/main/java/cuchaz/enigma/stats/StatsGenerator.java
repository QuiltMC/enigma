package cuchaz.enigma.stats;

import com.google.common.base.Preconditions;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.ArgumentDescriptor;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldDefEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.translation.representation.entry.ParentedEntry;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class StatsGenerator {
	private final EnigmaProject project;
	private final EntryIndex entryIndex;
	private final EntryResolver entryResolver;
	private ProjectStatsResult result = null;

	private CountDownLatch generationLatch = null;

	public StatsGenerator(EnigmaProject project) {
		this.project = project;
		this.entryIndex = project.getJarIndex().getEntryIndex();
		this.entryResolver = project.getJarIndex().getEntryResolver();
	}

	public ProjectStatsResult getResultNullable() {
		return this.result;
	}

	public ProjectStatsResult getResult(boolean includeSynthetic) {
		if (this.result == null) {
			return this.generateForClassTree(ProgressListener.none(), null, includeSynthetic);
		}

		return this.result;
	}

	/**
	 * Generates stats for the given class. Includes all {@link StatType}s in the calculation.
	 * @param progress a listener to update with current progress
	 * @param entry the class to generate stats for
	 * @param includeSynthetic whether to include synthetic methods
	 * @return the generated {@link StatsResult} for the provided class
	 */
	public ProjectStatsResult generateForClassTree(ProgressListener progress, ClassEntry entry, boolean includeSynthetic) {
		return this.generate(progress, EnumSet.allOf(StatType.class), entry, includeSynthetic);
	}

	/**
	 * Generates stats for the given package.
	 * @param progress a listener to update with current progress
	 * @param includedTypes the types of entry to include in the stats
	 * @param includeSynthetic whether to include synthetic methods
	 * @return the generated {@link StatsResult} for the provided package
	 */
	public ProjectStatsResult generate(ProgressListener progress, Set<StatType> includedTypes, boolean includeSynthetic) {
		return this.generate(progress, includedTypes, null, includeSynthetic);
	}

	/**
	 * Generates stats for the given package or class.
	 * @param progress a listener to update with current progress
	 * @param includedTypes the types of entry to include in the stats
	 * @param classEntry if stats are being generated for a single class, provide the class here
	 * @param includeSynthetic whether to include synthetic methods
	 * @return the generated {@link StatsResult} for the provided class or package.
	 */
	public ProjectStatsResult generate(ProgressListener progress, Set<StatType> includedTypes, @Nullable ClassEntry classEntry, boolean includeSynthetic) {
		includedTypes = EnumSet.copyOf(includedTypes);
		Map<ClassEntry, StatsResult> stats = this.result == null ? new HashMap<>() : this.result.getStats();

		if (this.result == null || classEntry == null) {
			if (this.generationLatch == null) {
				this.generationLatch = new CountDownLatch(1);

				Collection<ClassEntry> classes = this.entryIndex.getClasses();
				for (ClassEntry entry : classes) {
					if (!entry.isInnerClass()) {
						StatsResult result = this.generateOptimised(progress, includedTypes, entry, includeSynthetic);
						stats.put(entry, result);
					}
				}

				this.result = new ProjectStatsResult(this.project, stats);
				this.generationLatch.countDown();
			} else {
				try {
					this.generationLatch.await();
				} catch (InterruptedException e) {
					Logger.error(e, "Failed to await stats generation for project!");
				}
			}
		} else {
			Preconditions.checkNotNull(classEntry, "Entry cannot be null after initial stat generation!");
			stats.put(classEntry, this.generateOptimised(progress, includedTypes, classEntry, includeSynthetic));
			this.result = new ProjectStatsResult(this.project, stats);
		}

		return this.result;
	}

	public StatsResult generateOptimised(ProgressListener progress, Set<StatType> includedTypes, ClassEntry classEntry, boolean includeSynthetic) {
		Map<StatType, Integer> mappableCounts = new EnumMap<>(StatType.class);
		Map<StatType, Map<ClassEntry, Integer>> unmappedCounts = new EnumMap<>(StatType.class);

		boolean doneSearch = false;
		Set<ClassEntry> checked = new HashSet<>();
		List<ParentedEntry<?>> children = this.project.getJarIndex().getChildrenByClass().get(classEntry);
		List<Entry<?>> entries = new ArrayList<>(children);

		while (!doneSearch) {
			doneSearch = true;

			for (Entry<?> entry : children) {
				if (entry instanceof ClassEntry classEntry2 && !checked.contains(classEntry2)) {
					List<ParentedEntry<?>> classChildren = this.project.getJarIndex().getChildrenByClass().get(classEntry2);
					if (!classChildren.isEmpty()) {
						entries.addAll(this.project.getJarIndex().getChildrenByClass().get(classEntry2));
						doneSearch = false;
						checked.add(classEntry2);
					}
				}
			}
		}

		entries.add(classEntry);

		int numDone = 0;
		progress.init(entries.size(), "stats for class " + classEntry.getName());

		for (Entry<?> entry : entries) {
			progress.step(numDone++, "stats for class " + classEntry.getName());

			if (entry instanceof FieldEntry field) {
				if (!((FieldDefEntry) field).getAccess().isSynthetic()) {
					this.update(StatType.FIELDS, mappableCounts, unmappedCounts, field);
				}
			} else if (entry instanceof MethodEntry method) {
				MethodEntry root = this.entryResolver
					.resolveEntry(method, ResolutionStrategy.RESOLVE_ROOT)
					.stream()
					.findFirst()
					.orElseThrow(AssertionError::new);

				if (root == method) {
					if (includedTypes.contains(StatType.METHODS) && !((MethodDefEntry) method).getAccess().isSynthetic()) {
						this.update(StatType.METHODS, mappableCounts, unmappedCounts, method);
					}

					ClassEntry containingClass = method.getContainingClass();
					if (includedTypes.contains(StatType.PARAMETERS) && !this.project.isAnonymousOrLocal(containingClass) && !(((MethodDefEntry) method).getAccess().isSynthetic() && !includeSynthetic)) {
						MethodDescriptor descriptor = method.getDesc();
						List<ArgumentDescriptor> argumentDescs = descriptor.getArgumentDescs();

						int index = ((MethodDefEntry) method).getAccess().isStatic() ? 0 : 1;
						for (ArgumentDescriptor argument : argumentDescs) {
							if (!(argument.getAccess().isSynthetic() && !includeSynthetic)
								// skip the implicit superclass parameter for non-static inner class constructors
								&& !(method.isConstructor() && containingClass.isInnerClass() && index == 1 && argument.containsType() && argument.getTypeEntry().equals(containingClass.getOuterClass()))) {
								this.update(StatType.PARAMETERS, mappableCounts, unmappedCounts, new LocalVariableEntry(method, index, "", true, null));
							}

							index += argument.getSize();
						}
					}
				}
			} else if (entry instanceof ClassEntry clazz) {
				this.update(StatType.CLASSES, mappableCounts, unmappedCounts, clazz);
			}
		}

		Map<StatType, Integer> rawUnmappedCounts = new EnumMap<>(StatType.class);
		for (var entry : unmappedCounts.entrySet()) {
			for (int value : entry.getValue().values()) {
				rawUnmappedCounts.put(entry.getKey(), rawUnmappedCounts.getOrDefault(entry.getKey(), 0) + value);
			}
		}

		return new StatsResult(classEntry, mappableCounts, rawUnmappedCounts);
	}

//	private static StatsResult.Tree<Integer> getStatTree(Map<StatType, Map<String, Integer>> unmappedCounts, String topLevelPackageDot) {
//		// todo rewrite
//		StatsResult.Tree<Integer> tree = new StatsResult.Tree<>();
//
//		for (Map.Entry<StatType, Map<String, Integer>> typedEntry : unmappedCounts.entrySet()) {
//			for (Map.Entry<String, Integer> entry : typedEntry.getValue().entrySet()) {
//				if (entry.getKey().startsWith(topLevelPackageDot)) {
//					StatsResult.Tree.Node<Integer> node = tree.getNode(entry.getKey());
//					int value = node.getValue() == null ? 0 : node.getValue();
//
//					node.setValue(value + entry.getValue());
//				}
//			}
//		}
//
//		return tree;
//	}

	public StatsResult getStats(ClassEntry entry) {
		if (this.result == null) {
			return null;
		}

		return this.result.getStats().get(entry);
	}

	public void invalidateStats() {
		this.result = null;
	}

	private void update(StatType type, Map<StatType, Integer> mappable, Map<StatType, Map<ClassEntry, Integer>> unmapped, Entry<?> entry) {
		boolean obfuscated = this.project.isObfuscated(entry);
		boolean renamable = this.project.isRenamable(entry);
		boolean synthetic = this.project.isSynthetic(entry);

		if (renamable) {
			if (obfuscated && !synthetic) {
				ClassEntry parent = entry.getTopLevelClass();

				unmapped.computeIfAbsent(type, t -> new HashMap<>());
				unmapped.get(type).put(parent, unmapped.get(type).getOrDefault(parent, 0) + 1);
			}

			mappable.put(type, mappable.getOrDefault(type, 0) + 1);
		}
	}
}

package cuchaz.enigma.stats;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.translation.mapping.EntryRemapper;
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
import cuchaz.enigma.utils.I18n;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatsGenerator {
	private final EnigmaProject project;
	private final EntryIndex entryIndex;
	private final EntryRemapper mapper;
	private final EntryResolver entryResolver;

	public StatsGenerator(EnigmaProject project) {
		this.project = project;
		this.entryIndex = project.getJarIndex().getEntryIndex();
		this.mapper = project.getMapper();
		this.entryResolver = project.getJarIndex().getEntryResolver();
	}

	/**
	 * Generates stats for the given class. Includes all {@link StatType}s in the calculation.
	 * @param progress a listener to update with current progress
	 * @param entry the class to generate stats for
	 * @param includeSynthetic whether to include synthetic methods
	 * @return the generated {@link StatsResult} for the provided class
	 */
	public StatsResult generateForClassTree(ProgressListener progress, ClassEntry entry, boolean includeSynthetic) {
		return this.generate(progress, EnumSet.allOf(StatType.class), entry.getFullName(), entry, includeSynthetic);
	}

	/**
	 * Generates stats for the given package.
	 * @param progress a listener to update with current progress
	 * @param includedTypes the types of entry to include in the stats
	 * @param topLevelPackage the package to generate stats for. If this is empty, stats will be generated for the entire project.
	 * @param includeSynthetic whether to include synthetic methods
	 * @return the generated {@link StatsResult} for the provided package
	 */
	public StatsResult generate(ProgressListener progress, Set<StatType> includedTypes, String topLevelPackage, boolean includeSynthetic) {
		return this.generate(progress, includedTypes, topLevelPackage, null, includeSynthetic);
	}

	/**
	 * Generates stats for the given package or class.
	 * @param progress a listener to update with current progress
	 * @param includedTypes the types of entry to include in the stats
	 * @param topLevelPackage the package or class to generate stats for
	 * @param classEntry if stats are being generated for a single class, provide the class here
	 * @param includeSynthetic whether to include synthetic methods
	 * @return the generated {@link StatsResult} for the provided class or package.
	 */
	public StatsResult generate(ProgressListener progress, Set<StatType> includedTypes, String topLevelPackage, @Nullable ClassEntry classEntry, boolean includeSynthetic) {
		includedTypes = EnumSet.copyOf(includedTypes);
		int totalWork = 0;
		Map<StatType, Integer> mappableCounts = new EnumMap<>(StatType.class);
		Map<StatType, Map<String, Integer>> unmappedCounts = new EnumMap<>(StatType.class);

		if (includedTypes.contains(StatType.METHODS) || includedTypes.contains(StatType.PARAMETERS)) {
			totalWork += this.entryIndex.getMethods().size();
		}

		if (includedTypes.contains(StatType.FIELDS)) {
			totalWork += this.entryIndex.getFields().size();
		}

		if (includedTypes.contains(StatType.CLASSES)) {
			totalWork += this.entryIndex.getClasses().size();
		}

		progress.init(totalWork, I18n.translate("progress.stats"));

		String topLevelPackageSlash = topLevelPackage.replace(".", "/");

		int numDone = 0;
		if (includedTypes.contains(StatType.METHODS) || includedTypes.contains(StatType.PARAMETERS)) {
			for (MethodEntry method : this.entryIndex.getMethods()) {
				progress.step(numDone++, I18n.translate("type.methods"));

				MethodEntry root = this.entryResolver
						.resolveEntry(method, ResolutionStrategy.RESOLVE_ROOT)
						.stream()
						.findFirst()
						.orElseThrow(AssertionError::new);

				ClassEntry clazz = root.getParent();

				if (root == method && this.checkPackage(clazz, topLevelPackageSlash, classEntry)) {
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
			}
		}

		if (includedTypes.contains(StatType.FIELDS)) {
			for (FieldEntry field : this.entryIndex.getFields()) {
				progress.step(numDone++, I18n.translate("type.fields"));
				ClassEntry clazz = field.getParent();

				if (!((FieldDefEntry) field).getAccess().isSynthetic() && this.checkPackage(clazz, topLevelPackageSlash, classEntry)) {
					this.update(StatType.FIELDS, mappableCounts, unmappedCounts, field);
				}
			}
		}

		if (includedTypes.contains(StatType.CLASSES)) {
			for (ClassEntry clazz : this.entryIndex.getClasses()) {
				progress.step(numDone++, I18n.translate("type.classes"));

				if (this.checkPackage(clazz, topLevelPackageSlash, classEntry)) {
					this.update(StatType.CLASSES, mappableCounts, unmappedCounts, clazz);
				}
			}
		}

		progress.step(-1, I18n.translate("progress.stats.data"));

		// generate html display
		StatsResult.Tree<Integer> tree = new StatsResult.Tree<>();

		for (Map.Entry<StatType, Map<String, Integer>> typedEntry : unmappedCounts.entrySet()) {
			for (Map.Entry<String, Integer> entry : typedEntry.getValue().entrySet()) {
				if (entry.getKey().startsWith(topLevelPackage)) {
					StatsResult.Tree.Node<Integer> node = tree.getNode(entry.getKey());
					int value = node.getValue() == null ? 0 : node.getValue();

					node.setValue(value + entry.getValue());
				}
			}
		}

		tree.collapse(tree.root);

		Map<StatType, Integer> rawUnmappedCounts = new EnumMap<>(StatType.class);
		for (var entry : unmappedCounts.entrySet()) {
			for (int value : entry.getValue().values()) {
				rawUnmappedCounts.put(entry.getKey(), rawUnmappedCounts.getOrDefault(entry.getKey(), 0) + value);
			}
		}

		return new StatsResult(mappableCounts, rawUnmappedCounts, tree);
	}

	private boolean checkPackage(ClassEntry clazz, String topLevelPackage, @Nullable ClassEntry entry) {
		String packageName = this.mapper.deobfuscate(clazz).getPackageName();

		if (entry == null) {
			return topLevelPackage.isBlank() || (packageName != null && packageName.startsWith(topLevelPackage));
		} else {
			return clazz.getTopLevelClass().equals(entry.getTopLevelClass());
		}
	}

	private void update(StatType type, Map<StatType, Integer> mappable, Map<StatType, Map<String, Integer>> unmapped, Entry<?> entry) {
		boolean obfuscated = this.project.isObfuscated(entry);
		boolean renamable = this.project.isRenamable(entry);
		boolean synthetic = this.project.isSynthetic(entry);

		if (renamable) {
			if (obfuscated && !synthetic) {
				String parent = this.mapper.deobfuscate(entry.getTopLevelClass()).getName().replace('/', '.');

				unmapped.computeIfAbsent(type, t -> new HashMap<>());
				unmapped.get(type).put(parent, unmapped.get(type).getOrDefault(parent, 0) + 1);
			}

			mappable.put(type, mappable.getOrDefault(type, 0) + 1);
		}
	}
}

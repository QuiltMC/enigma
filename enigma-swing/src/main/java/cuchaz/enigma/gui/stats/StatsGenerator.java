package cuchaz.enigma.gui.stats;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldDefEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;

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

	public StatsResult generateForClassTree(ProgressListener progress, ClassEntry entry, boolean includeSynthetic) {
		return generate(progress, EnumSet.allOf(StatsMember.class), entry.getFullName(), true, includeSynthetic);
	}

	public StatsResult generate(ProgressListener progress, Set<StatsMember> includedMembers, String topLevelPackage, boolean includeSynthetic) {
		return generate(progress, includedMembers, topLevelPackage, false, includeSynthetic);
	}

	/**
	 * Generates stats for the given package or class.
	 * @param progress a listener to update with current progress
	 * @param includedMembers the types of entry to include in the stats
	 * @param topLevelPackage the package or class to generate stats for
	 * @param forClassTree if true, the stats will be generated for the class tree - this means that non-mappable obfuscated entries will be ignored for correctness
	 * @param includeSynthetic whether to include synthetic methods
	 * @return the generated {@link StatsResult} for the provided class or package.
	 */
	public StatsResult generate(ProgressListener progress, Set<StatsMember> includedMembers, String topLevelPackage, boolean forClassTree, boolean includeSynthetic) {
		includedMembers = EnumSet.copyOf(includedMembers);
		int totalWork = 0;
		int totalMappable = 0;

		if (includedMembers.contains(StatsMember.METHODS) || includedMembers.contains(StatsMember.PARAMETERS)) {
			totalWork += this.entryIndex.getMethods().size();
		}

		if (includedMembers.contains(StatsMember.FIELDS)) {
			totalWork += this.entryIndex.getFields().size();
		}

		if (includedMembers.contains(StatsMember.CLASSES)) {
			totalWork += this.entryIndex.getClasses().size();
		}

		progress.init(totalWork, I18n.translate("progress.stats"));

		Map<String, Integer> counts = new HashMap<>();

		String topLevelPackageSlash = topLevelPackage.replace(".", "/");

		int numDone = 0;
		if (includedMembers.contains(StatsMember.METHODS) || includedMembers.contains(StatsMember.PARAMETERS)) {
			for (MethodEntry method : this.entryIndex.getMethods()) {
				progress.step(numDone++, I18n.translate("type.methods"));

				// we don't want constructors or otherwise non-mappable things to show as a mapped method!
				if (!project.isRenamable(method)) {
					continue;
				}

				MethodEntry root = this.entryResolver
						.resolveEntry(method, ResolutionStrategy.RESOLVE_ROOT)
						.stream()
						.findFirst()
						.orElseThrow(AssertionError::new);

                ClassEntry clazz = root.getParent();

                if (root == method && checkPackage(clazz, topLevelPackageSlash, forClassTree)) {
                    if (includedMembers.contains(StatsMember.METHODS) && !((MethodDefEntry) method).getAccess().isSynthetic()) {
						totalMappable += this.update(counts, method, forClassTree);
                    }

					if (includedMembers.contains(StatsMember.PARAMETERS) && (!((MethodDefEntry) method).getAccess().isSynthetic() || includeSynthetic)) {
						int index = ((MethodDefEntry) method).getAccess().isStatic() ? 0 : 1;
						for (TypeDescriptor argument : method.getDesc().getArgumentDescs()) {
							totalMappable += this.update(counts, new LocalVariableEntry(method, index, "", true, null), forClassTree);
							index += argument.getSize();
						}
					}
				}
			}
		}

        if (includedMembers.contains(StatsMember.FIELDS)) {
            for (FieldEntry field : this.entryIndex.getFields()) {
                progress.step(numDone++, I18n.translate("type.fields"));
                ClassEntry clazz = field.getParent();

                if (!((FieldDefEntry) field).getAccess().isSynthetic() && checkPackage(clazz, topLevelPackageSlash, forClassTree)) {
					totalMappable += this.update(counts, field, forClassTree);
                }
            }
        }

        if (includedMembers.contains(StatsMember.CLASSES)) {
            for (ClassEntry clazz : this.entryIndex.getClasses()) {
                progress.step(numDone++, I18n.translate("type.classes"));

                if (checkPackage(clazz, topLevelPackageSlash, forClassTree)) {
					totalMappable += this.update(counts, clazz, forClassTree);
                }
            }
        }

		progress.step(-1, I18n.translate("progress.stats.data"));

		StatsResult.Tree<Integer> tree = new StatsResult.Tree<>();

		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			if (entry.getKey().startsWith(topLevelPackage)) {
				tree.getNode(entry.getKey()).value = entry.getValue();
			}
		}

		tree.collapse(tree.root);
		return new StatsResult(totalMappable, counts.values().stream().mapToInt(i -> i).sum(), tree);
	}

	private boolean checkPackage(ClassEntry clazz, String topLevelPackage, boolean singleClass) {
		String deobfuscatedName = this.mapper.deobfuscate(clazz).getPackageName();
		if (singleClass) {
			return (deobfuscatedName != null && deobfuscatedName.startsWith(topLevelPackage)) || clazz.getFullName().startsWith(topLevelPackage);
		}

		return topLevelPackage.isBlank() || (deobfuscatedName != null && deobfuscatedName.startsWith(topLevelPackage));
	}

	/**
	 * @return whether to increment the total mappable entry count - 0 if no, 1 if yes
	 */
	private int update(Map<String, Integer> counts, Entry<?> entry, boolean forClassTree) {
		boolean obfuscated = this.project.isObfuscated(entry);
		boolean renamable = this.project.isRenamable(entry);
		boolean synthetic = this.project.isSynthetic(entry);

		if (forClassTree && obfuscated && !renamable) {
			return 0;
		}

		if (obfuscated && renamable && !synthetic) {
			String parent = this.mapper.deobfuscate(entry.getAncestry().get(0)).getName().replace('/', '.');
			counts.put(parent, counts.getOrDefault(parent, 0) + 1);
			return 1;
		}

		return 1;
	}
}

package org.quiltmc.enigma.api.stats;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.service.EnigmaService;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.representation.ArgumentDescriptor;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ParentedEntry;
import org.quiltmc.enigma.util.I18n;
import org.tinylog.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class StatsGenerator {
	private final EnigmaProject project;
	private final EntryIndex entryIndex;
	private final EntryResolver entryResolver;

	private ProjectStatsResult result = null;
	private GenerationParameters lastParameters = new GenerationParameters();
	private ProgressListener overallListener;
	private CountDownLatch generationLatch = null;

	private List<String> fallbackNameProposerIdCache;

	public StatsGenerator(EnigmaProject project) {
		this.project = project;
		this.entryIndex = project.getJarIndex().getIndex(EntryIndex.class);
		this.entryResolver = project.getJarIndex().getEntryResolver();
	}

	/**
	 * Gets the latest generated stats.
	 * @return the stats, or {@code null} if not yet generated
	 */
	@Nullable
	public ProjectStatsResult getResultNullable() {
		return this.result;
	}

	/**
	 * Gets the latest generated stats, or generates them if not available.
	 * Regenerates stats if parameters have changed.
	 * @return the stats
	 */
	public ProjectStatsResult getResult(GenerationParameters parameters) {
		System.out.println(this.lastParameters);
		System.out.println(parameters);

		if (this.result == null || !this.lastParameters.equals(parameters)) {
			return this.generate(ProgressListener.createEmpty(), parameters);
		}

		return this.result;
	}

	/**
	 * If stats are currently being generated, returns the progress listener.
	 * @return the current progress
	 */
	@Nullable
	public ProgressListener getOverallProgress() {
		return this.overallListener;
	}

	/**
	 * Generates stats for the current project.
	 * @param progress a listener to update with current progress
	 * @param parameters the parameters to use for generation
	 * @return the generated {@link ProjectStatsResult}
	 */
	public ProjectStatsResult generate(ProgressListener progress, GenerationParameters parameters) {
		return this.generate(progress, null, parameters);
	}

	/**
	 * Generates stats for the current project or updates existing stats with the provided class.
	 * Somewhat thread-safe: will only generate stats on one thread at a time, awaiting generation on all other threads if called in parallel.
	 * @param progress a listener to update with current progress
	 * @param classEntry if stats are being generated for a single class, provide the class here
	 * @param parameters the parameters to use for generation
	 * @return the generated {@link ProjectStatsResult} for the provided class or package
	 */
	public ProjectStatsResult generate(ProgressListener progress, @Nullable ClassEntry classEntry, GenerationParameters parameters) {
		if (classEntry == null && this.overallListener == null) {
			this.overallListener = progress;
		}

		this.rebuildCache();

		Map<ClassEntry, StatsResult> stats = this.result == null ? new HashMap<>() : this.result.getStats();

		if (classEntry == null) {
			if (this.generationLatch == null || this.generationLatch.getCount() == 0) {
				this.generationLatch = new CountDownLatch(1);

				List<ClassEntry> classes = this.entryIndex.getClasses()
					.stream().filter(entry -> !entry.isInnerClass()).toList();

				int done = 0;
				progress.init(classes.size() - 1, I18n.translate("progress.stats"));

				for (ClassEntry entry : classes) {
					progress.step(done++, I18n.translateFormatted("progress.stats.for", entry.getName()));
					StatsResult result = this.generate(entry, parameters, false);
					stats.put(entry, result);
				}

				this.result = new ProjectStatsResult(this.project, stats);
				this.lastParameters = parameters;
				this.generationLatch.countDown();
			} else {
				try {
					progress.init(1, "progress.stats.awaiting");
					this.generationLatch.await();
				} catch (InterruptedException e) {
					Logger.error(e, "Failed to await stats generation for project!");
				}
			}
		} else {
			stats.put(classEntry, this.generate(classEntry, parameters, false));
			this.result = new ProjectStatsResult(this.project, stats);
			this.lastParameters = parameters;
		}

		this.overallListener = null;
		return this.result;
	}

	private void addChildrenRecursively(List<Entry<?>> entries, Entry<?> toCheck) {
		if (toCheck instanceof ClassEntry innerClassEntry) {
			List<ParentedEntry<?>> classChildren = this.project.getJarIndex().getChildrenByClass().get(innerClassEntry);
			if (!classChildren.isEmpty()) {
				entries.addAll(classChildren);
				for (Entry<?> entry : classChildren) {
					if (entry instanceof ClassEntry innerInnerClassEntry) {
						this.addChildrenRecursively(entries, innerInnerClassEntry);
					}
				}
			}
		}
	}

	/**
	 * Generates stats for the provided class.
	 * @param classEntry the class to generate stats for
	 * @param parameters the parameters to use for generation
	 * @return the generated {@link StatsResult}
	 */
	public StatsResult generate(ClassEntry classEntry, GenerationParameters parameters) {
		return this.generate(classEntry, parameters, true);
	}

	private StatsResult generate(ClassEntry classEntry, GenerationParameters parameters, boolean rebuildCache) {
		if (rebuildCache) {
			this.rebuildCache();
		}

		Map<StatType, Integer> mappableCounts = new EnumMap<>(StatType.class);
		Map<StatType, Map<String, Integer>> unmappedCounts = new EnumMap<>(StatType.class);

		List<ParentedEntry<?>> children = this.project.getJarIndex().getChildrenByClass().get(classEntry);
		List<Entry<?>> entries = new ArrayList<>(children);

		for (Entry<?> entry : children) {
			this.addChildrenRecursively(entries, entry);
		}

		entries.add(classEntry);

		Set<StatType> includedTypes = parameters.includedTypes();

		for (Entry<?> entry : entries) {
			if (entry instanceof FieldEntry field && includedTypes.contains(StatType.FIELDS)) {
				if (!((FieldDefEntry) field).getAccess().isSynthetic()) {
					this.update(StatType.FIELDS, mappableCounts, unmappedCounts, field, parameters);
				}
			} else if (entry instanceof MethodEntry method) {
				MethodEntry root = this.entryResolver
						.resolveEntry(method, ResolutionStrategy.RESOLVE_ROOT)
						.stream()
						.findFirst()
						.orElseThrow(AssertionError::new);

				if (root == method) {
					if (includedTypes.contains(StatType.METHODS) && !((MethodDefEntry) method).getAccess().isSynthetic()) {
						this.update(StatType.METHODS, mappableCounts, unmappedCounts, method, parameters);
					}

					ClassEntry containingClass = method.getContainingClass();
					if (includedTypes.contains(StatType.PARAMETERS) && !this.project.isAnonymousOrLocal(containingClass) && !(((MethodDefEntry) method).getAccess().isSynthetic() && !parameters.includeSynthetic())) {
						ClassDefEntry def = this.entryIndex.getDefinition(containingClass);
						if (def != null && def.isRecord()) {
							if (this.isCanonicalConstructor(def, method)
									|| method.equals(new MethodEntry(containingClass, "equals", new MethodDescriptor("(Ljava/lang/Object;)Z")))) {
								continue;
							}
						}

						MethodDescriptor descriptor = method.getDesc();
						List<ArgumentDescriptor> argumentDescs = descriptor.getArgumentDescs();

						int index = ((MethodDefEntry) method).getAccess().isStatic() ? 0 : 1;
						for (ArgumentDescriptor argument : argumentDescs) {
							if (!(argument.getAccess().isSynthetic() && !parameters.includeSynthetic())
									// skip the implicit superclass parameter for non-static inner class constructors
									&& !(method.isConstructor() && containingClass.isInnerClass() && index == 1 && argument.containsType() && argument.getTypeEntry().equals(containingClass.getOuterClass()))) {
								this.update(StatType.PARAMETERS, mappableCounts, unmappedCounts, new LocalVariableEntry(method, index), parameters);
							}

							index += argument.getSize();
						}
					}
				}
			} else if (entry instanceof ClassEntry clazz && includedTypes.contains(StatType.CLASSES)) {
				this.update(StatType.CLASSES, mappableCounts, unmappedCounts, clazz, parameters);
			}
		}

		return StatsResult.create(mappableCounts, unmappedCounts, false);
	}

	private void rebuildCache() {
		this.fallbackNameProposerIdCache = this.project.getEnigma().getNameProposalServices().stream().filter(NameProposalService::isFallback).map(EnigmaService::getId).toList();
	}

	private boolean isCanonicalConstructor(ClassDefEntry record, MethodEntry methodEntry) {
		if (!record.isRecord() || !methodEntry.isConstructor()) {
			return false;
		}

		MethodDescriptor descriptor = methodEntry.getDesc();
		List<ArgumentDescriptor> argumentDescs = descriptor.getArgumentDescs();
		List<FieldEntry> fields = this.project.getJarIndex().getChildrenByClass().get(record).stream()
				.filter(e -> {
					if (e instanceof FieldEntry field) {
						var access = this.entryIndex.getFieldAccess(field);
						return access != null && !access.isSynthetic() && !access.isStatic();
					}

					return false;
				})
				.map(e -> (FieldEntry) e)
				.toList();

		// number of parameters must match the number of fields
		if (argumentDescs.size() != fields.size()) {
			return false;
		}

		// match types
		for (int i = 0; i < fields.size(); i++) {
			FieldEntry field = fields.get(i);
			ArgumentDescriptor argument = argumentDescs.get(i);

			if (!field.getDesc().toString().equals(argument.toString())) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Gets the stats for the provided class.
	 * @param entry the class to get stats for
	 * @return the stats, or {@code null} if not generated
	 */
	public StatsResult getStats(ClassEntry entry) {
		if (this.result == null) {
			return null;
		}

		return this.result.getStats().get(entry);
	}

	private void update(StatType type, Map<StatType, Integer> mappable, Map<StatType, Map<String, Integer>> unmapped, Entry<?> entry, GenerationParameters parameters) {
		if (this.project.isRenamable(entry)) {
			boolean obf = this.project.isObfuscated(entry);

			if ((obf && (!this.project.isSynthetic(entry) || !parameters.includeSynthetic()))
					|| (!parameters.countFallback() && this.fallbackNameProposerIdCache.contains(this.project.getRemapper().getMapping(entry).sourcePluginId()))) { // fallback proposed mappings don't count
				String parent = this.project.getRemapper().deobfuscate(entry.getTopLevelClass()).getName().replace('/', '.');

				unmapped.computeIfAbsent(type, t -> new HashMap<>());
				unmapped.get(type).put(parent, unmapped.get(type).getOrDefault(parent, 0) + 1);
			}

			mappable.put(type, mappable.getOrDefault(type, 0) + 1);
		}
	}
}

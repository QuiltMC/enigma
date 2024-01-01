package org.quiltmc.enigma.api.translation.mapping;

import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.InheritanceIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.mapping.MappingsIndex;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.translation.MappingTranslator;
import org.quiltmc.enigma.api.translation.Translatable;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.tree.DeltaTrackingTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.MergedEntryMappingTree;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.validation.Message;
import org.quiltmc.enigma.util.validation.ValidationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EntryRemapper {
	private final EntryTree<EntryMapping> deobfMappings;
	private final EntryTree<EntryMapping> jarProposedMappings;
	private final EntryTree<EntryMapping> proposedMappings;
	private final DeltaTrackingTree<EntryMapping> mappings;

	private final EntryResolver obfResolver;
	private final Translator deobfuscator;
	private final JarIndex jarIndex;
	private final MappingsIndex mappingsIndex;

	private final MappingValidator validator;
	private final List<NameProposalService> proposalServices;

	private EntryRemapper(JarIndex jarIndex, MappingsIndex mappingsIndex, EntryTree<EntryMapping> jarProposedMappings, EntryTree<EntryMapping> deobfMappings, List<NameProposalService> proposalServices) {
		this.deobfMappings = deobfMappings;
		this.jarProposedMappings = jarProposedMappings;
		this.proposedMappings = new HashEntryTree<>(jarProposedMappings);
		this.mappings = new DeltaTrackingTree<>(new MergedEntryMappingTree(deobfMappings, this.proposedMappings));

		this.obfResolver = jarIndex.getEntryResolver();

		this.deobfuscator = new MappingTranslator(this.mappings, this.obfResolver);
		this.jarIndex = jarIndex;
		this.mappingsIndex = mappingsIndex;

		this.validator = new MappingValidator(this.deobfuscator, jarIndex, mappingsIndex);
		this.proposalServices = proposalServices;
	}

	public static EntryRemapper mapped(JarIndex jarIndex, MappingsIndex mappingsIndex, EntryTree<EntryMapping> proposedMappings, EntryTree<EntryMapping> deobfMappings, List<NameProposalService> proposalServices) {
		return new EntryRemapper(jarIndex, mappingsIndex, proposedMappings, deobfMappings, proposalServices);
	}

	public static EntryRemapper empty(JarIndex index, List<NameProposalService> proposalServices) {
		return new EntryRemapper(index, MappingsIndex.empty(), new HashEntryTree<>(), new HashEntryTree<>(), proposalServices);
	}

	public void validatePutMapping(ValidationContext vc, Entry<?> obfuscatedEntry, @Nonnull EntryMapping deobfMapping) {
		this.doPutMapping(vc, obfuscatedEntry, deobfMapping, true);
	}

	public void putMapping(ValidationContext vc, Entry<?> obfuscatedEntry, @Nonnull EntryMapping deobfMapping) {
		this.doPutMapping(vc, obfuscatedEntry, deobfMapping, false);
	}

	// note: just supressing warnings until it's fixed
	@SuppressWarnings("all")
	private void doPutMapping(ValidationContext vc, Entry<?> obfuscatedEntry, @Nonnull EntryMapping deobfMapping, boolean validateOnly) {
		// todo this needs to be fixed!
		//if (obfuscatedEntry instanceof FieldEntry fieldEntry) {
		//	mapRecordComponentGetter(vc, fieldEntry.getParent(), fieldEntry, deobfMapping);
		//}

		EntryMapping oldMapping = this.getMapping(obfuscatedEntry);
		boolean renaming = !Objects.equals(oldMapping.targetName(), deobfMapping.targetName());

		Collection<Entry<?>> resolvedEntries = renaming ? resolveAllRoots(obfuscatedEntry) : this.obfResolver.resolveEntry(obfuscatedEntry, ResolutionStrategy.RESOLVE_CLOSEST);

		if (renaming && deobfMapping.targetName() != null) {
			for (Entry<?> resolvedEntry : resolvedEntries) {
				this.validator.validateRename(vc, resolvedEntry, deobfMapping.targetName());
			}
		}

		if (validateOnly || !vc.canProceed()) return;

		for (Entry<?> resolvedEntry : resolvedEntries) {
			if (deobfMapping.equals(EntryMapping.DEFAULT)) {
				this.mappings.insert(resolvedEntry, null);
			} else {
				this.mappings.insert(resolvedEntry, deobfMapping);
			}
		}

		this.insertDynamicallyProposedMappings(obfuscatedEntry, oldMapping, deobfMapping);
		this.mappingsIndex.reindexEntry(deobfMapping, obfuscatedEntry);
	}

	private Collection<Entry<?>> resolveAllRoots(Entry<?> obfuscatedEntry) {
		if (!(obfuscatedEntry instanceof MethodEntry methodEntry)) {
			return this.obfResolver.resolveEntry(obfuscatedEntry, ResolutionStrategy.RESOLVE_ROOT);
		}

		InheritanceIndex inheritanceIndex = this.jarIndex.getIndex(InheritanceIndex.class);
		var owner = methodEntry.getParent();
		var descendants = inheritanceIndex.getDescendants(owner);
		var knownParents = new HashSet<>(inheritanceIndex.getParents(owner));

		// Find all classes with an "unknown" parent, so we can also resolve the method from there and find other definitions
		// If interfaces A and B define method `void foo()`, a class C may implement both interfaces, having a single method `void foo()`
		// and effectively "joining" the two interface methods, so you have to keep both "in sync"
		List<ClassEntry> classes = new ArrayList<>();
		for (ClassEntry descendant : descendants) {
			var parents = inheritanceIndex.getParents(descendant);
			if (parents.size() > 1) { // one of them is one of the owner's descendants
				Set<ClassEntry> otherParents = new HashSet<>(parents);
				otherParents.removeAll(descendants);
				otherParents.removeAll(knownParents);
				if (!otherParents.isEmpty()) {
					classes.add(descendant);
					knownParents.addAll(otherParents);
				}
			}
		}

		Set<Entry<?>> resolution = new HashSet<>(this.obfResolver.resolveEntry(obfuscatedEntry, ResolutionStrategy.RESOLVE_ROOT));
		for (ClassEntry clazz : classes) {
			resolution.addAll(this.obfResolver.resolveEntry(methodEntry.withParent(clazz), ResolutionStrategy.RESOLVE_ROOT));
		}

		return resolution;
	}

	// todo this needs to be fixed for hashed mappings!
	// note: just supressing warnings until it's fixed
	@SuppressWarnings("all")
	private void mapRecordComponentGetter(ValidationContext vc, ClassEntry classEntry, FieldEntry fieldEntry, EntryMapping fieldMapping) {
		EntryIndex entryIndex = this.jarIndex.getIndex(EntryIndex.class);

		if (!entryIndex.getDefinition(classEntry).isRecord() || entryIndex.getFieldAccess(fieldEntry).isStatic()) {
			return;
		}

		// Find all the methods in this record class
		List<MethodEntry> classMethods = entryIndex.getMethods().stream()
				.filter(entry -> classEntry.equals(entry.getParent()))
				.toList();

		MethodEntry methodEntry = null;

		for (MethodEntry method : classMethods) {
			// Find the matching record component getter via matching the names. TODO: Support when the record field and method names do not match
			if (method.getName().equals(fieldEntry.getName()) && method.getDesc().toString().equals("()" + fieldEntry.getDesc())) {
				methodEntry = method;
				break;
			}
		}

		if (methodEntry == null && fieldMapping != null) {
			vc.raise(Message.UNKNOWN_RECORD_GETTER, fieldMapping.targetName());
			return;
		}

		// Also remap the associated method, without the javadoc.
		this.doPutMapping(vc, methodEntry, new EntryMapping(fieldMapping.targetName()), false);
	}

	/**
	 * Runs {@link NameProposalService#getDynamicProposedNames(EntryRemapper, Entry, EntryMapping, EntryMapping)} over the names stored in this remapper,
	 * inserting all mappings generated.
	 */
	public void insertDynamicallyProposedMappings(@Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping) {
		for (var service : this.proposalServices) {
			var proposedNames = service.getDynamicProposedNames(this, obfEntry, oldMapping, newMapping);
			if (proposedNames != null) {
				proposedNames.forEach(this.proposedMappings::insert);
			}
		}
	}

	@Nonnull
	public EntryMapping getMapping(Entry<?> entry) {
		EntryMapping entryMapping = this.mappings.get(entry);
		return entryMapping == null ? EntryMapping.DEFAULT : entryMapping;
	}

	public <T extends Translatable> TranslateResult<T> extendedDeobfuscate(T translatable) {
		return this.deobfuscator.extendedTranslate(translatable);
	}

	public <T extends Translatable> T deobfuscate(T translatable) {
		return this.deobfuscator.translate(translatable);
	}

	public Translator getDeobfuscator() {
		return this.deobfuscator;
	}

	public Stream<Entry<?>> getObfEntries() {
		return this.mappings.getAllEntries();
	}

	public Collection<Entry<?>> getObfChildren(Entry<?> obfuscatedEntry) {
		return this.mappings.getChildren(obfuscatedEntry);
	}

	/**
	 * Gets all mappings, including both manually inserted and proposed names.
	 * @return the merged mapping tree
	 */
	public DeltaTrackingTree<EntryMapping> getMappings() {
		return this.mappings;
	}

	/**
	 * Gets all manually inserted mappings.
	 * @return the deobfuscated mapping tree
	 */
	public EntryTree<EntryMapping> getDeobfMappings() {
		return this.deobfMappings;
	}

	/**
	 * Gets all proposed mappings.
	 * @return the proposed mapping tree
	 */
	public EntryTree<EntryMapping> getProposedMappings() {
		return this.proposedMappings;
	}

	/**
	 * Gets mappings proposed at the jar indexing stage.
	 * @return the proposed mapping tree
	 */
	public EntryTree<EntryMapping> getJarProposedMappings() {
		return this.jarProposedMappings;
	}

	public MappingDelta<EntryMapping> takeMappingDelta() {
		return this.mappings.takeDelta();
	}

	public boolean isDirty() {
		return this.mappings.isDirty();
	}

	public EntryResolver getObfResolver() {
		return this.obfResolver;
	}

	public MappingValidator getValidator() {
		return this.validator;
	}

	public JarIndex getJarIndex() {
		return this.jarIndex;
	}
}

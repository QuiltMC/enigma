package org.quiltmc.enigma.api.translation.mapping;

import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
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
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.validation.Message;
import org.quiltmc.enigma.util.validation.ValidationContext;

import java.util.Collection;
import java.util.Objects;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EntryRemapper {
	private final DeltaTrackingTree<EntryMapping> obfToDeobf;

	private final EntryResolver obfResolver;
	private final Translator deobfuscator;
	private final JarIndex jarIndex;
	private final MappingsIndex mappingsIndex;

	private final MappingValidator validator;
	private final List<NameProposalService> proposalServices;

	private EntryRemapper(JarIndex jarIndex, MappingsIndex mappingsIndex, EntryTree<EntryMapping> obfToDeobf, List<NameProposalService> proposalServices) {
		this.obfToDeobf = new DeltaTrackingTree<>(obfToDeobf);

		this.obfResolver = jarIndex.getEntryResolver();

		this.deobfuscator = new MappingTranslator(obfToDeobf, this.obfResolver);
		this.jarIndex = jarIndex;
		this.mappingsIndex = mappingsIndex;

		this.validator = new MappingValidator(this.deobfuscator, jarIndex, mappingsIndex);
		this.proposalServices = proposalServices;
	}

	public static EntryRemapper mapped(JarIndex jarIndex, MappingsIndex mappingsIndex, EntryTree<EntryMapping> obfToDeobf, List<NameProposalService> proposalServices) {
		return new EntryRemapper(jarIndex, mappingsIndex, obfToDeobf, proposalServices);
	}

	public static EntryRemapper empty(JarIndex index, List<NameProposalService> proposalServices) {
		return new EntryRemapper(index, MappingsIndex.empty(), new HashEntryTree<>(), proposalServices);
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

		Collection<Entry<?>> resolvedEntries = this.obfResolver.resolveEntry(obfuscatedEntry, renaming ? ResolutionStrategy.RESOLVE_ROOT : ResolutionStrategy.RESOLVE_CLOSEST);

		if (renaming && deobfMapping.targetName() != null) {
			for (Entry<?> resolvedEntry : resolvedEntries) {
				this.validator.validateRename(vc, resolvedEntry, deobfMapping.targetName());
			}
		}

		if (validateOnly || !vc.canProceed()) return;

		for (Entry<?> resolvedEntry : resolvedEntries) {
			if (deobfMapping.equals(EntryMapping.DEFAULT)) {
				this.obfToDeobf.insert(resolvedEntry, null);
			} else {
				this.obfToDeobf.insert(resolvedEntry, deobfMapping);
			}
		}

		this.insertDynamicallyProposedMappings(obfuscatedEntry, oldMapping, deobfMapping);
		this.mappingsIndex.reindexEntry(deobfMapping, obfuscatedEntry);
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
				proposedNames.forEach(this.obfToDeobf::insert);
			}
		}
	}

	@Nonnull
	public EntryMapping getMapping(Entry<?> entry) {
		EntryMapping entryMapping = this.obfToDeobf.get(entry);
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
		return this.obfToDeobf.getAllEntries();
	}

	public Collection<Entry<?>> getObfChildren(Entry<?> obfuscatedEntry) {
		return this.obfToDeobf.getChildren(obfuscatedEntry);
	}

	public DeltaTrackingTree<EntryMapping> getObfToDeobf() {
		return this.obfToDeobf;
	}

	public MappingDelta<EntryMapping> takeMappingDelta() {
		return this.obfToDeobf.takeDelta();
	}

	public boolean isDirty() {
		return this.obfToDeobf.isDirty();
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

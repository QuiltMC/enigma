package org.quiltmc.enigma.api.translation.mapping;

import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.mapping.MappingsIndex;
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

public class EntryRemapper {
	private final DeltaTrackingTree<EntryMapping> obfToDeobf;

	private final EntryResolver obfResolver;
	private final Translator deobfuscator;
	private final JarIndex jarIndex;

	private final MappingValidator validator;

	private EntryRemapper(JarIndex jarIndex, MappingsIndex mappingsIndex, EntryTree<EntryMapping> obfToDeobf) {
		this.obfToDeobf = new DeltaTrackingTree<>(obfToDeobf);

		this.obfResolver = jarIndex.getEntryResolver();

		this.deobfuscator = new MappingTranslator(obfToDeobf, this.obfResolver);
		this.jarIndex = jarIndex;

		this.validator = new MappingValidator(this.deobfuscator, jarIndex, mappingsIndex);
	}

	public static EntryRemapper mapped(JarIndex jarIndex, MappingsIndex mappingsIndex, EntryTree<EntryMapping> obfToDeobf) {
		return new EntryRemapper(jarIndex, mappingsIndex, obfToDeobf);
	}

	public static EntryRemapper empty(JarIndex index) {
		return new EntryRemapper(index, MappingsIndex.empty(), new HashEntryTree<>());
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

		boolean renaming = !Objects.equals(this.getDeobfMapping(obfuscatedEntry).targetName(), deobfMapping.targetName());

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
	}

	// todo this needs to be fixed for hashed mappings!
	// note: just supressing warnings until it's fixed
	@SuppressWarnings("all")
	private void mapRecordComponentGetter(ValidationContext vc, ClassEntry classEntry, FieldEntry fieldEntry, EntryMapping fieldMapping) {
		if (!this.jarIndex.getEntryIndex().getDefinition(classEntry).isRecord() || this.jarIndex.getEntryIndex().getFieldAccess(fieldEntry).isStatic()) {
			return;
		}

		// Find all the methods in this record class
		List<MethodEntry> classMethods = this.jarIndex.getEntryIndex().getMethods().stream()
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

	@Nonnull
	public EntryMapping getDeobfMapping(Entry<?> entry) {
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

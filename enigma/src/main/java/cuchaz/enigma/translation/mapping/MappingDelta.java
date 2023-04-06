package cuchaz.enigma.translation.mapping;

import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.util.stream.Stream;

public class MappingDelta<T> implements Translatable {
	public static final Object PLACEHOLDER = new Object();

	private final EntryTree<T> baseMappings;

	private final EntryTree<Object> changes;

	public MappingDelta(EntryTree<T> baseMappings, EntryTree<Object> changes) {
		this.baseMappings = baseMappings;
		this.changes = changes;
	}

	public MappingDelta(EntryTree<T> baseMappings) {
		this(baseMappings, new HashEntryTree<>());
	}

	public static <T> MappingDelta<T> added(EntryTree<T> mappings) {
		EntryTree<Object> changes = new HashEntryTree<>();
		mappings.getAllEntries().forEach(entry -> changes.insert(entry, PLACEHOLDER));

		return new MappingDelta<>(new HashEntryTree<>(), changes);
	}

	public EntryTree<T> getBaseMappings() {
		return this.baseMappings;
	}

	public EntryTree<?> getChanges() {
		return this.changes;
	}

	public Stream<Entry<?>> getChangedRoots() {
		return this.changes.getRootNodes().map(EntryTreeNode::getEntry);
	}

	@Override
	public TranslateResult<MappingDelta<T>> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		// there's no concept of deobfuscated for this as far as I can see, so
		// it will always be marked as obfuscated
		return TranslateResult.ungrouped(new MappingDelta<>(
				translator.translate(this.baseMappings),
				translator.translate(this.changes)
		));
	}
}

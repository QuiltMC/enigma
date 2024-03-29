package org.quiltmc.enigma.api.translation.mapping.tree;

import org.quiltmc.enigma.api.translation.Translatable;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMap;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import java.util.Collection;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public interface EntryTree<T> extends EntryMap<T>, Iterable<EntryTreeNode<T>>, Translatable {
	Collection<Entry<?>> getChildren(Entry<?> entry);

	Collection<Entry<?>> getSiblings(Entry<?> entry);

	@Nullable
	EntryTreeNode<T> findNode(Entry<?> entry);

	Stream<EntryTreeNode<T>> getRootNodes();

	@Override
	default TranslateResult<? extends EntryTree<T>> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		return TranslateResult.ungrouped(this.translate(translator, resolver, mappings));
	}

	@Override
	EntryTree<T> translate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings);
}

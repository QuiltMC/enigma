package org.quiltmc.enigma.util;

import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.VoidEntryResolver;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.mapping.tree.HashEntryTree;
import org.quiltmc.enigma.api.translation.MappingTranslator;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.HashSet;
import java.util.Set;

public class MappingOperations {
	public static EntryTree<EntryMapping> invert(EntryTree<EntryMapping> mappings) {
		Translator translator = new MappingTranslator(mappings, VoidEntryResolver.INSTANCE);
		EntryTree<EntryMapping> result = new HashEntryTree<>();

		for (EntryTreeNode<EntryMapping> node : mappings) {
			Entry<?> leftEntry = node.getEntry();
			EntryMapping leftMapping = node.getValue();

			if (!(leftEntry instanceof ClassEntry || leftEntry instanceof MethodEntry || leftEntry instanceof FieldEntry)) {
				result.insert(translator.translate(leftEntry), leftMapping);
				continue;
			}

			Entry<?> rightEntry = translator.translate(leftEntry);

			result.insert(rightEntry, leftMapping == null ? null : leftMapping.withName(leftEntry.getName()));
		}

		return result;
	}

	public static EntryTree<EntryMapping> compose(EntryTree<EntryMapping> left, EntryTree<EntryMapping> right, boolean keepLeftOnly, boolean keepRightOnly) {
		Translator leftTranslator = new MappingTranslator(left, VoidEntryResolver.INSTANCE);
		EntryTree<EntryMapping> result = new HashEntryTree<>();
		Set<Entry<?>> addedMappings = new HashSet<>();

		for (EntryTreeNode<EntryMapping> node : left) {
			Entry<?> leftEntry = node.getEntry();
			EntryMapping leftMapping = node.getValue();

			Entry<?> rightEntry = leftTranslator.translate(leftEntry);

			EntryMapping rightMapping = right.get(rightEntry);
			if (rightMapping != null) {
				result.insert(leftEntry, rightMapping);
				addedMappings.add(rightEntry);
			} else if (keepLeftOnly) {
				result.insert(leftEntry, leftMapping);
			}
		}

		if (keepRightOnly) {
			Translator leftInverseTranslator = new MappingTranslator(invert(left), VoidEntryResolver.INSTANCE);
			for (EntryTreeNode<EntryMapping> node : right) {
				Entry<?> rightEntry = node.getEntry();
				EntryMapping rightMapping = node.getValue();

				if (!addedMappings.contains(rightEntry)) {
					result.insert(leftInverseTranslator.translate(rightEntry), rightMapping);
				}
			}
		}

		return result;
	}
}

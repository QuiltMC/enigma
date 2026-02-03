package org.quiltmc.enigma.impl.plugin;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.HashMap;
import java.util.Map;

public record RecordComponentProposalService(RecordIndexingVisitor visitor) implements NameProposalService {
	public static final String ID = "enigma:record_component_proposer";

	@Nullable
	@Override
	public Map<Entry<?>, EntryMapping> getProposedNames(Enigma enigma, JarIndex index) {
		return null;
	}

	@Nullable
	@Override
	public Map<Entry<?>, EntryMapping> getDynamicProposedNames(
			EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping,
			@Nullable EntryMapping newMapping
	) {
		if (obfEntry instanceof FieldEntry fieldEntry) {
			return this.mapRecordComponentGetter(fieldEntry, newMapping);
		} else if (obfEntry == null) {
			final Map<Entry<?>, EntryMapping> mappings = new HashMap<>();
			for (final EntryTreeNode<EntryMapping> mapping : remapper.getMappings()) {
				if (mapping.getEntry() instanceof FieldEntry fieldEntry) {
					final Map<Entry<?>, EntryMapping> getter =
							this.mapRecordComponentGetter(fieldEntry, mapping.getValue());
					if (getter != null) {
						mappings.putAll(getter);
					}
				}
			}

			return mappings;
		}

		return null;
	}

	@Nullable
	private Map<Entry<?>, EntryMapping> mapRecordComponentGetter(FieldEntry obfFieldEntry, EntryMapping mapping) {
		final MethodEntry obfGetter = this.visitor.getComponentGetter(obfFieldEntry);
		if (obfGetter == null) {
			return null;
		}

		// remap method to match field
		final EntryMapping getterMapping = mapping.tokenType() == TokenType.OBFUSCATED
				? EntryMapping.OBFUSCATED
				: this.createMapping(mapping.targetName(), TokenType.DYNAMIC_PROPOSED);
		return Map.of(obfGetter, getterMapping);
	}

	@Override
	public void validateProposedMapping(Entry<?> entry, EntryMapping mapping, boolean dynamic) {
		if (dynamic && mapping.tokenType() == TokenType.OBFUSCATED) {
			return;
		}

		NameProposalService.super.validateProposedMapping(entry, mapping, dynamic);
	}

	@Override
	public String getId() {
		return ID;
	}
}

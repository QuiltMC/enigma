package org.quiltmc.enigma.impl.plugin;

import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class RecordComponentProposalService implements NameProposalService {
	@Nullable
	@Override
	public Map<Entry<?>, EntryMapping> getProposedNames(JarIndex index) {
		return null;
	}

	@Nullable
	@Override
	public Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping) {
		if (obfEntry instanceof FieldEntry fieldEntry) {
			return this.mapRecordComponentGetter(remapper, fieldEntry.getContainingClass(), fieldEntry, newMapping);
		}

		return null;
	}

	@Nullable
	private Map<Entry<?>, EntryMapping> mapRecordComponentGetter(EntryRemapper remapper, ClassEntry parent, FieldEntry obfFieldEntry, EntryMapping mapping) {
		EntryIndex entryIndex = remapper.getJarIndex().getIndex(EntryIndex.class);
		ClassDefEntry parentDef = entryIndex.getDefinition(parent);
		var def = entryIndex.getDefinition(obfFieldEntry);
		if ((parentDef != null && !parentDef.isRecord()) || (def != null && def.getAccess().isStatic())) {
			return null;
		}

		MethodEntry obfMethodEntry = null;
		List<MethodEntry> obfClassMethods = remapper.getJarIndex().getChildrenByClass().get(parentDef).stream()
				.filter(e -> e instanceof MethodEntry)
				.map(e -> (MethodEntry) e)
				.toList();

		for (MethodEntry method : obfClassMethods) {
			if (method.getName().equals(obfFieldEntry.getName()) && method.getDesc().toString().equals("()" + obfFieldEntry.getDesc())) {
				obfMethodEntry = method;
				break;
			}
		}

		if (obfMethodEntry == null) {
			return null;
		}

		// remap method to match field
		EntryMapping newMapping = mapping.tokenType() == TokenType.OBFUSCATED ? new EntryMapping(null, null, TokenType.OBFUSCATED, null) : this.createMapping(mapping.targetName(), TokenType.DYNAMIC_PROPOSED);
		return Map.of(obfMethodEntry, newMapping);
	}

	@Override
	public String getId() {
		return "enigma:record_component_proposer";
	}
}

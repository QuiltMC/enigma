package org.quiltmc.enigma.impl.plugin;

import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class EnumConstantProposalService implements NameProposalService {
	private final EnumFieldNameFindingVisitor visitor;

	EnumConstantProposalService(EnumFieldNameFindingVisitor visitor) {
		this.visitor = visitor;
	}

	@Override
	public Map<Entry<?>, EntryMapping> getProposedNames(Enigma enigma, JarIndex index) {
		Map<Entry<?>, EntryMapping> mappings = new HashMap<>();

		index.getIndex(EntryIndex.class).getFields().forEach(field -> {
			final String name = this.visitor.getEnumConstantName(field);
			if (name != null) {
				mappings.put(field, this.createMapping(name, TokenType.JAR_PROPOSED));
			}
		});

		return mappings;
	}

	@Override
	public Map<Entry<?>, EntryMapping> getDynamicProposedNames(
			EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping,
			@Nullable EntryMapping newMapping
	) {
		return null;
	}

	@Override
	public String getId() {
		return "enigma:enum_name_proposer";
	}
}

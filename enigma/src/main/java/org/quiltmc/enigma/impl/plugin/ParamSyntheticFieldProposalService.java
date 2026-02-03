package org.quiltmc.enigma.impl.plugin;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import java.util.Map;

public class ParamSyntheticFieldProposalService implements NameProposalService {
	private final ParamSyntheticFieldIndexingVisitor visitor;

	ParamSyntheticFieldProposalService(ParamSyntheticFieldIndexingVisitor visitor) {
		this.visitor = visitor;
	}

	@Override
	@Nullable
	public Map<Entry<?>, EntryMapping> getProposedNames(Enigma enigma, JarIndex index) {
		return null;
	}

	@Override
	@Nullable
	public Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping) {
		// TODO
		return Map.of();
	}

	@Override
	public String getId() {
		return "enigma:param_synthetic_field_name_proposer";
	}
}

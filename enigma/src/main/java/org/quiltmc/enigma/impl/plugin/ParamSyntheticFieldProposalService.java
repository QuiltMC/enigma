package org.quiltmc.enigma.impl.plugin;

import org.jspecify.annotations.Nullable;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParamSyntheticFieldProposalService implements NameProposalService {
	public static final String ID = "enigma:param_synthetic_field_name_proposer";

	private final ParamSyntheticFieldIndexingService indexer;

	ParamSyntheticFieldProposalService(ParamSyntheticFieldIndexingService indexer) {
		this.indexer = indexer;
	}

	@Override
	@Nullable
	public Map<Entry<?>, EntryMapping> getProposedNames(Enigma enigma, JarIndex index) {
		return null;
	}

	@Override
	@Nullable
	public Map<Entry<?>, EntryMapping> getDynamicProposedNames(
			EntryRemapper remapper, @Nullable Entry<?> obfEntry,
			@Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping
	) {
		if (obfEntry == null) {
			return this.indexer.streamSyntheticFieldLinkedParams()
				.flatMap(entry -> {
					final EntryMapping mapping = remapper.getMapping(entry.getKey());
					return mapping.tokenType() == TokenType.OBFUSCATED ? Stream.empty()
							: Stream.of(Map.entry(entry.getValue(), withMetaInfo(mapping)));
				})
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		} else if (obfEntry instanceof LocalVariableEntry local) {
			final FieldEntry field = this.indexer.getLinkedSyntheticField(local);
			if (field != null) {
				if (newMapping != null && newMapping.tokenType() != TokenType.OBFUSCATED) {
					return Map.of(field, withMetaInfo(newMapping));
				}
			}
		}

		return null;
	}

	private static EntryMapping withMetaInfo(EntryMapping mapping) {
		return new EntryMapping(mapping.targetName(), mapping.javadoc(), TokenType.DYNAMIC_PROPOSED, ID);
	}

	@Override
	public String getId() {
		return ID;
	}
}

package org.quiltmc.enigma.api.translation;

import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nullable;

public class ProposingTranslator implements Translator {
	private final EntryRemapper mapper;
	private final NameProposalService[] nameProposalServices;

	public ProposingTranslator(EntryRemapper mapper, NameProposalService[] nameProposalServices) {
		this.mapper = mapper;
		this.nameProposalServices = nameProposalServices;
	}

	@Nullable
	@Override
	@SuppressWarnings("unchecked")
	public <T extends Translatable> TranslateResult<T> extendedTranslate(T translatable) {
		if (translatable == null) {
			return null;
		}

		TranslateResult<T> deobfuscated = this.mapper.extendedDeobfuscate(translatable);

		if (translatable instanceof Entry && ((Entry<?>) deobfuscated.getValue()).getName().equals(((Entry<?>) translatable).getName())) {
			return this.mapper.getObfResolver()
					.resolveEntry((Entry<?>) translatable, ResolutionStrategy.RESOLVE_ROOT)
					.stream()
					.map(this::proposeName)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.findFirst()
					.map(newName -> TranslateResult.proposed((T) ((Entry<?>) deobfuscated.getValue()).withName(newName)))
					.orElse(deobfuscated);
		}

		return deobfuscated;
	}

	private Optional<String> proposeName(Entry<?> entry) {
		return Arrays.stream(this.nameProposalServices)
				.map(service -> service.proposeName(entry, this.mapper))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst();
	}
}
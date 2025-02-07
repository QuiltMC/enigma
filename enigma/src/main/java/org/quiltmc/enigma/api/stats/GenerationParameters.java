package org.quiltmc.enigma.api.stats;

import org.quiltmc.enigma.api.service.NameProposalService;

import java.util.EnumSet;
import java.util.Set;

/**
 * Defines the parameters to be used when generating statistics via a {@link StatsGenerator}.
 * @param includedTypes the {@link StatType stat types} to include in the result
 * @param includeSynthetic whether to include <a href=https://docs.oracle.com/javase/tutorial/java/generics/bridgeMethods.html>synthetic entries</a> in the result
 * @param countFallback whether to count {@link NameProposalService#isFallback() fallback-proposed} entries as mapped in the result
 */
public record GenerationParameters(Set<StatType> includedTypes, boolean includeSynthetic, boolean countFallback) {
	public GenerationParameters() {
		this(EnumSet.noneOf(StatType.class), false, false);
	}

	public GenerationParameters(Set<StatType> types) {
		this(types, false, false);
	}

	public GenerationParameters(Set<StatType> types, boolean includeSynthetic) {
		this(types, includeSynthetic, false);
	}
}

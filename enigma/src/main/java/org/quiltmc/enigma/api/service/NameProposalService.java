package org.quiltmc.enigma.api.service;

import org.checkerframework.checker.units.qual.N;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * A name proposal service suggests default names for entries based on context from their types and surrounding mappings.
 */
public interface NameProposalService extends EnigmaService {
	EnigmaServiceType<NameProposalService> TYPE = EnigmaServiceType.create("name_proposal");

	/**
	 * Runs when a new JAR file is opened. Note that at this point, no mapping context will exist in the remapper.
	 * All mappings proposed should have a token type of {@link org.quiltmc.enigma.api.source.RenamableTokenType#JAR_PROPOSED} and a non-null source plugin ID.
	 *
	 * @param index an index of the jar, to use as context
	 * @return a map of obfuscated entries to their proposed names
	 */
	@Nullable
	Map<Entry<?>, EntryMapping> getProposedNames(JarIndex index);

	/**
	 * Runs when an entry is renamed, for updating proposed names that use other mappings as context.
	 * Is also run when new mappings are opened -- in that case, {@code obfEntry}, {@code oldMapping}, and {@code newMapping} will be null.
	 * All mappings proposed should have a token type of {@link org.quiltmc.enigma.api.source.RenamableTokenType#DYNAMIC_PROPOSED} and a non-null source plugin ID.
	 *
	 * @param remapper a remapper to use as context for name proposal
	 * @param obfEntry the obfuscated entry that was renamed
	 * @param oldMapping the old mapping
	 * @param newMapping the new mapping
	 * @return a map of obfuscated entries to their proposed names
	 */
	@Nullable
	Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping);
}

package org.quiltmc.enigma.api.service;

import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * A name proposal service suggests default names for entries based on context from their types and surrounding mappings.
 * <br>
 * Name proposal services are not active by default, and need to be specified in the {@link org.quiltmc.enigma.api.EnigmaProfile profile}.
 */
public interface NameProposalService extends EnigmaService {
	EnigmaServiceType<NameProposalService> TYPE = EnigmaServiceType.create("name_proposal");

	/**
	 * Runs when a new JAR file is opened. Note that at this point, no mapping context will exist in the remapper.
	 * All mappings proposed should have a token type of {@link TokenType#JAR_PROPOSED} and a non-null source plugin ID.
	 *
	 * @param index an index of the jar, to use as context
	 * @return a map of obfuscated entries to their proposed names
	 */
	@Nullable
	Map<Entry<?>, EntryMapping> getProposedNames(JarIndex index);

	/**
	 * Runs when an entry is renamed, for updating proposed names that use other mappings as context.
	 * Is also run when new mappings are opened -- in that case, {@code obfEntry}, {@code oldMapping}, and {@code newMapping} will be null.
	 * Will not be run for insertion of proposed mappings, only manual renames.
	 * All mappings proposed should have a token type of {@link TokenType#DYNAMIC_PROPOSED} and a non-null source plugin ID.
	 *
	 * @param remapper a remapper to use as context for name proposal
	 * @param obfEntry the obfuscated entry that was renamed
	 * @param oldMapping the old mapping
	 * @param newMapping the new mapping
	 * @return a map of obfuscated entries to their proposed names
	 */
	@Nullable
	Map<Entry<?>, EntryMapping> getDynamicProposedNames(EntryRemapper remapper, @Nullable Entry<?> obfEntry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping);

	default boolean isFallback() {
		return false;
	}

	default boolean alwaysSave() {
		return false;
	}

	/**
	 * Creates a proposed mapping, with no javadoc and using {@link #getId()} as the source plugin ID.
	 * @param name the name
	 * @param tokenType the token type - must be either {@link TokenType#JAR_PROPOSED} or {@link TokenType#DYNAMIC_PROPOSED}
	 * @return the newly created mapping
	 */
	default EntryMapping createMapping(String name, TokenType tokenType) {
		if (!tokenType.isProposed()) {
			throw new IllegalArgumentException("token type must be proposed!");
		}

		return new EntryMapping(name, null, tokenType, this.getId());
	}
}

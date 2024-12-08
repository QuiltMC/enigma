package org.quiltmc.enigma.api.service;

import org.quiltmc.enigma.api.Enigma;
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
	 * @param enigma an enigma instance to use as context
	 * @param index an index of the jar, to use as context
	 * @return a map of obfuscated entries to their proposed names
	 */
	@Nullable
	Map<Entry<?>, EntryMapping> getProposedNames(Enigma enigma, JarIndex index);

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

	/**
	 * Disables validation of proposed mappings from this service.
	 * This allows you to return any kind of mapping you want from {@link #getDynamicProposedNames(EntryRemapper, Entry, EntryMapping, EntryMapping)}
	 * and {@link #getProposedNames(Enigma, JarIndex)}, but should be used sparingly as it will allow creating mappings that can't be linked back to this proposer.
	 * Do not use this unless you're sure there's no other way to accomplish what you're looking to do!
	 *
	 * @return whether validation should be bypassed
	 */
	default boolean bypassValidation() {
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

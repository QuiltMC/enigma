package org.quiltmc.enigma.api.service;

import org.quiltmc.enigma.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;

import java.util.Optional;

public interface NameProposalService extends EnigmaService {
	EnigmaServiceType<NameProposalService> TYPE = EnigmaServiceType.create("name_proposal");

	Optional<String> proposeName(Entry<?> obfEntry, EntryRemapper remapper);
}

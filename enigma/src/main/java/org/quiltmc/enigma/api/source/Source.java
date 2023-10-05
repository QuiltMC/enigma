package org.quiltmc.enigma.api.source;

import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;

public interface Source {
	String asString();

	Source withJavadocs(EntryRemapper remapper);

	SourceIndex index();
}

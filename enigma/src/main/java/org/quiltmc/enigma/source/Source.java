package org.quiltmc.enigma.source;

import org.quiltmc.enigma.translation.mapping.EntryRemapper;

public interface Source {
	String asString();

	Source withJavadocs(EntryRemapper remapper);

	SourceIndex index();
}

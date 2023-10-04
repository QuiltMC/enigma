package org.quiltmc.enigma.translation.representation.entry;

import org.quiltmc.enigma.translation.representation.AccessFlags;

public interface DefEntry<P extends Entry<?>> extends Entry<P> {
	AccessFlags getAccess();
}

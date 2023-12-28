package org.quiltmc.enigma.api.translation.representation.entry;

import org.quiltmc.enigma.api.translation.representation.AccessFlags;

public interface DefEntry<P extends Entry<?>> extends Entry<P> {
	AccessFlags getAccess();
}

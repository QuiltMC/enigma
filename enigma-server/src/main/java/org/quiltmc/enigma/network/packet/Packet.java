package org.quiltmc.enigma.network.packet;

import java.io.DataOutput;
import java.io.IOException;

public interface Packet<H> {
	void write(DataOutput output) throws IOException;

	void handle(H handler);
}

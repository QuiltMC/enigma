package org.quiltmc.enigma.source.bytecode;

import org.quiltmc.enigma.Enigma;
import org.quiltmc.enigma.source.SourceIndex;
import org.objectweb.asm.util.Textifier;

public class EnigmaTextifier extends Textifier {
	private final SourceIndex sourceIndex;

	public EnigmaTextifier(SourceIndex sourceIndex) {
		super(Enigma.ASM_VERSION);
		this.sourceIndex = sourceIndex;
	}

	public void clearText() {
		this.text.clear();
	}
}

package org.quiltmc.enigma.util;

import org.objectweb.asm.tree.analysis.Value;

public record LocalVariableValue(int size, boolean parameter, int local) implements Value {
	public LocalVariableValue(int size) {
		this(size, false, -1);
	}

	public LocalVariableValue(int size, LocalVariableValue value) {
		this(size, value.parameter, value.local);
	}

	@Override
	public int getSize() {
		return this.size;
	}
}

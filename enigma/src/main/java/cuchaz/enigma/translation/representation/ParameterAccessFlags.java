package cuchaz.enigma.translation.representation;

import org.objectweb.asm.Opcodes;

public class ParameterAccessFlags {
	public static final ParameterAccessFlags DEFAULT = new ParameterAccessFlags(0);

	private int flags;

	public ParameterAccessFlags(int flags) {
		this.flags = flags;
	}

	public boolean isSynthetic() {
		return (this.flags & Opcodes.ACC_SYNTHETIC) != 0;
	}

	public boolean isFinal() {
		return (this.flags & Opcodes.ACC_FINAL) != 0;
	}
}

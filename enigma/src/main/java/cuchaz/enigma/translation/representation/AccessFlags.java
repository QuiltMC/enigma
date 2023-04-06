package cuchaz.enigma.translation.representation;

import cuchaz.enigma.analysis.Access;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

public class AccessFlags {
	public static final AccessFlags PRIVATE = new AccessFlags(Opcodes.ACC_PRIVATE);
	public static final AccessFlags PUBLIC = new AccessFlags(Opcodes.ACC_PUBLIC);

	private int flags;

	public AccessFlags(int flags) {
		this.flags = flags;
	}

	public boolean isPrivate() {
		return Modifier.isPrivate(this.flags);
	}

	public boolean isProtected() {
		return Modifier.isProtected(this.flags);
	}

	public boolean isPublic() {
		return Modifier.isPublic(this.flags);
	}

	public boolean isSynthetic() {
		return (this.flags & Opcodes.ACC_SYNTHETIC) != 0;
	}

	public boolean isStatic() {
		return Modifier.isStatic(this.flags);
	}

	public boolean isEnum() {
		return (this.flags & Opcodes.ACC_ENUM) != 0;
	}

	public boolean isBridge() {
		return (this.flags & Opcodes.ACC_BRIDGE) != 0;
	}

	public boolean isFinal() {
		return (this.flags & Opcodes.ACC_FINAL) != 0;
	}

	public boolean isInterface() {
		return (this.flags & Opcodes.ACC_INTERFACE) != 0;
	}

	public boolean isAbstract() {
		return (this.flags & Opcodes.ACC_ABSTRACT) != 0;
	}

	public boolean isAnnotation() {
		return (this.flags & Opcodes.ACC_ANNOTATION) != 0;
	}

	public AccessFlags setPrivate() {
		this.setVisibility(Opcodes.ACC_PRIVATE);
		return this;
	}

	public AccessFlags setProtected() {
		this.setVisibility(Opcodes.ACC_PROTECTED);
		return this;
	}

	public AccessFlags setPublic() {
		this.setVisibility(Opcodes.ACC_PUBLIC);
		return this;
	}

	public AccessFlags setBridge() {
		this.flags |= Opcodes.ACC_BRIDGE;
		return this;
	}

	@Deprecated
	public AccessFlags setBridged() {
		return this.setBridge();
	}

	public void setVisibility(int visibility) {
		this.resetVisibility();
		this.flags |= visibility;
	}

	private void resetVisibility() {
		this.flags &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC);
	}

	public int getFlags() {
		return this.flags;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof AccessFlags accessFlags && accessFlags.flags == this.flags;
	}

	@Override
	public int hashCode() {
		return this.flags;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(Access.get(this).toString().toLowerCase());
		if (this.isStatic()) {
			builder.append(" static");
		}

		if (this.isSynthetic()) {
			builder.append(" synthetic");
		}

		if (this.isBridge()) {
			builder.append(" bridge");
		}

		return builder.toString();
	}
}

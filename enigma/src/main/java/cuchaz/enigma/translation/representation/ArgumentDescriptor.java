package cuchaz.enigma.translation.representation;

import java.util.function.UnaryOperator;

public class ArgumentDescriptor extends TypeDescriptor {
	private ParameterAccessFlags access;

	public ArgumentDescriptor(String desc, ParameterAccessFlags access) {
		super(desc);
		this.access = access;
	}

	public ParameterAccessFlags getAccess() {
		return this.access;
	}

	public void setAccess(ParameterAccessFlags access) {
		this.access = access;
	}

	public ArgumentDescriptor remap(UnaryOperator<String> remapper) {
		return new ArgumentDescriptor(super.remap(remapper).desc, this.getAccess());
	}
}

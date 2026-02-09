package org.quiltmc.enigma.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Interpreter;

import java.util.List;

/**
 * Track values as {@link LocalVariableValue local variables}.
 *
 * @author <a href="https://github.com/IotaBread">IotaBread</a>
 */
public class LocalVariableInterpreter extends Interpreter<LocalVariableValue> implements Opcodes {
	public static final LocalVariableInterpreter INSTANCE = new LocalVariableInterpreter();

	protected LocalVariableInterpreter() {
		super(Opcodes.ASM9);
	}

	@Override
	public LocalVariableValue newValue(Type type) {
		if (type == Type.VOID_TYPE) {
			// Only used in returns, must be null for void
			return null;
		}

		return new LocalVariableValue(type == null ? 1 : type.getSize());
	}

	@Override
	public LocalVariableValue newParameterValue(boolean isInstanceMethod, int local, Type type) {
		return new LocalVariableValue(type.getSize(), !isInstanceMethod || local > 0, local);
	}

	@Override
	public LocalVariableValue newEmptyValue(int local) {
		return new LocalVariableValue(1, false, local);
	}

	@Override
	public LocalVariableValue newOperation(AbstractInsnNode instruction) {
		return new LocalVariableValue(
				switch (instruction.getOpcode()) {
					case Opcodes.LCONST_0, Opcodes.LCONST_1, Opcodes.DCONST_0, Opcodes.DCONST_1 -> 2;
					case Opcodes.LDC -> {
						final Object value = ((LdcInsnNode) instruction).cst;
						yield value instanceof Double || value instanceof Long ? 2 : 1;
					}
					case Opcodes.GETSTATIC -> Type.getType(((FieldInsnNode) instruction).desc).getSize();
					default -> 1;
				}
		);
	}

	@Override
	public LocalVariableValue copyOperation(AbstractInsnNode instruction, LocalVariableValue value) {
		return value;
	}

	@Override
	public LocalVariableValue unaryOperation(AbstractInsnNode instruction, LocalVariableValue value) {
		return switch (instruction.getOpcode()) {
			// Widening casts (automatic) should keep the variable they came from
			case I2L, I2D, L2D, F2D -> new LocalVariableValue(2, value);
			case I2F, L2F -> new LocalVariableValue(1, value);

			case LNEG, DNEG, F2L, D2L -> new LocalVariableValue(2);
			case GETFIELD -> new LocalVariableValue(Type.getType(((FieldInsnNode) instruction).desc).getSize());
			default -> new LocalVariableValue(1);
		};
	}

	@Override
	public LocalVariableValue binaryOperation(
			AbstractInsnNode instruction, LocalVariableValue value1, LocalVariableValue value2
	) {
		return switch (instruction.getOpcode()) {
			case LALOAD, DALOAD, LADD, DADD, LSUB, DSUB, LMUL,
					DMUL, LDIV, DDIV,
					LREM, DREM, LSHL, LSHR, LUSHR, LAND, LOR,
					LXOR -> new LocalVariableValue(2);
			default -> new LocalVariableValue(1);
		};
	}

	@Override
	public LocalVariableValue ternaryOperation(
			AbstractInsnNode instruction,
			LocalVariableValue value1, LocalVariableValue value2, LocalVariableValue value3
	) {
		return new LocalVariableValue(1);
	}

	@Override
	public LocalVariableValue naryOperation(AbstractInsnNode instruction, List<? extends LocalVariableValue> values) {
		return new LocalVariableValue(
				switch (instruction.getOpcode()) {
					case Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKESTATIC, Opcodes.INVOKEINTERFACE ->
							Type.getReturnType(((MethodInsnNode) instruction).desc).getSize();
					case Opcodes.INVOKEDYNAMIC -> Type.getReturnType(((InvokeDynamicInsnNode) instruction).desc).getSize();
					default -> 1;
				}
		);
	}

	@Override
	public void returnOperation(AbstractInsnNode instruction, LocalVariableValue value, LocalVariableValue expected) {}

	@Override
	public LocalVariableValue merge(LocalVariableValue value1, LocalVariableValue value2) {
		return new LocalVariableValue(Math.min(value1.size(), value2.size()));
	}
}

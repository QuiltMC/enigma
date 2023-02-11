package cuchaz.enigma.analysis;

import cuchaz.enigma.Enigma;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

import java.util.List;
import java.util.Objects;

public class InterpreterPair<V extends Value, W extends Value> extends Interpreter<InterpreterPair.PairValue<V, W>> {
	private final Interpreter<V> left;
	private final Interpreter<W> right;

	public InterpreterPair(Interpreter<V> left, Interpreter<W> right) {
		super(Enigma.ASM_VERSION);
		this.left = left;
		this.right = right;
	}

	@Override
	public PairValue<V, W> newValue(Type type) {
		return this.pair(
				this.left.newValue(type),
				this.right.newValue(type)
		);
	}

	@Override
	public PairValue<V, W> newOperation(AbstractInsnNode insn) throws AnalyzerException {
		return this.pair(
				this.left.newOperation(insn),
				this.right.newOperation(insn)
		);
	}

	@Override
	public PairValue<V, W> copyOperation(AbstractInsnNode insn, PairValue<V, W> value) throws AnalyzerException {
		return this.pair(
				this.left.copyOperation(insn, value.left),
				this.right.copyOperation(insn, value.right)
		);
	}

	@Override
	public PairValue<V, W> unaryOperation(AbstractInsnNode insn, PairValue<V, W> value) throws AnalyzerException {
		return this.pair(
				this.left.unaryOperation(insn, value.left),
				this.right.unaryOperation(insn, value.right)
		);
	}

	@Override
	public PairValue<V, W> binaryOperation(AbstractInsnNode insn, PairValue<V, W> value1, PairValue<V, W> value2) throws AnalyzerException {
		return this.pair(
				this.left.binaryOperation(insn, value1.left, value2.left),
				this.right.binaryOperation(insn, value1.right, value2.right)
		);
	}

	@Override
	public PairValue<V, W> ternaryOperation(AbstractInsnNode insn, PairValue<V, W> value1, PairValue<V, W> value2, PairValue<V, W> value3) throws AnalyzerException {
		return this.pair(
				this.left.ternaryOperation(insn, value1.left, value2.left, value3.left),
				this.right.ternaryOperation(insn, value1.right, value2.right, value3.right)
		);
	}

	@Override
	public PairValue<V, W> naryOperation(AbstractInsnNode insn, List<? extends PairValue<V, W>> values) throws AnalyzerException {
		return this.pair(
				this.left.naryOperation(insn, values.stream().map(PairValue::left).toList()),
				this.right.naryOperation(insn, values.stream().map(PairValue::right).toList())
		);
	}

	@Override
	public void returnOperation(AbstractInsnNode insn, PairValue<V, W> value, PairValue<V, W> expected) throws AnalyzerException {
		this.left.returnOperation(insn, value.left, expected.left);
		this.right.returnOperation(insn, value.right, expected.right);
	}

	@Override
	public PairValue<V, W> merge(PairValue<V, W> value1, PairValue<V, W> value2) {
		return this.pair(
				this.left.merge(value1.left, value2.left),
				this.right.merge(value1.right, value2.right)
		);
	}

	private PairValue<V, W> pair(V left, W right) {
		if (left == null && right == null) {
			return null;
		}

		return new PairValue<>(left, right);
	}

	public record PairValue<V extends Value, W extends Value>(V left, W right) implements Value {
		public PairValue {
			if (left == null && right == null) {
				throw new IllegalArgumentException("should use null rather than pair of nulls");
			}
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof InterpreterPair.PairValue<? extends Value, ? extends Value> pairValue && Objects.equals(this.left, pairValue.left) && Objects.equals(this.right, pairValue.right);
		}

		@Override
		public int getSize() {
			return (this.left == null ? this.right : this.left).getSize();
		}
	}
}

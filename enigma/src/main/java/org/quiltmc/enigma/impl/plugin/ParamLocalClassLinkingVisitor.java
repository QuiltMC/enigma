package org.quiltmc.enigma.impl.plugin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.util.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ParamLocalClassLinkingVisitor extends ClassVisitor implements Opcodes {
	final Map<String, Multimap<MethodNode, TypeInstructionIndex>> localTypeInstructionsByMethodByOwner =
		new HashMap<>();
	// excludes no-args constructors
	final Map<String, Map<String, MethodNode>> localConstructorsByDescByOwner = new HashMap<>();
	final Map<String, Map<String, Map<String, FieldIndexOffset>>> localSyntheticFieldOffsetsByDescByNameByOwner =
		new HashMap<>();
	final Map<String, Multimap<MethodNode, FieldNode>> localSyntheticFieldsByGetterByOwner = new HashMap<>();

	private String className;
	private boolean classIsLocal;
	private int syntheticFieldOffset;
	private final Set<String> localClassNames = new HashSet<>();
	// only populated for local/anonymous classes and their outer classes
	private final List<MethodNode> methods = new LinkedList<>();

	ParamLocalClassLinkingVisitor() {
		super(Enigma.ASM_VERSION);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);

		this.className = name;
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		if (outerName == null) {
			if (this.className.equals(name)) {
				this.classIsLocal = true;
			} else {
				this.localClassNames.add(name);
			}
		}
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if (this.classIsLocal && (access & ACC_SYNTHETIC) != 0 && (access & ACC_STATIC) == 0) {
			final var node = new FieldNode(access, name, descriptor, signature, value);
			this.localSyntheticFieldOffsetsByDescByNameByOwner
					.computeIfAbsent(this.className, Utils::createHashMap)
					.computeIfAbsent(name, Utils::createHashMap)
					.put(descriptor, new FieldIndexOffset(node, this.syntheticFieldOffset++));

			return node;
		} else {
			return super.visitField(access, name, descriptor, signature, value);
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		final boolean hasLocalClasses = !this.localClassNames.isEmpty();
		if (this.classIsLocal || hasLocalClasses) {
			final MethodNode node = new MethodNode(this.api, access, name, descriptor, signature, exceptions);

			this.methods.add(node);

			if (this.classIsLocal) {
				if (name.equals("<init>")) {
					if (!descriptor.startsWith("()")) {
						this.localConstructorsByDescByOwner
								.computeIfAbsent(this.className, Utils::createHashMap)
								.put(descriptor, node);
					}
				}
			}

			return node;
		} else {
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}
	}

	@Override
	public void visitEnd() {
		super.visitEnd();

		this.collectResults();

		this.className = null;
		this.classIsLocal = false;
		this.syntheticFieldOffset = 0;
		this.localClassNames.clear();
		this.methods.clear();
	}

	private void collectResults() {
		if (!this.localClassNames.isEmpty()) {
			for (final MethodNode method : this.methods) {
				int index = 0;
				for (AbstractInsnNode instruction = method.instructions.getFirst();
						instruction != null;
						instruction = instruction.getNext()
				) {
					if (
							instruction instanceof TypeInsnNode typeInstruction
								&& typeInstruction.getOpcode() == NEW
								&& this.localClassNames.contains(typeInstruction.desc)
					) {
						this.localTypeInstructionsByMethodByOwner
								.computeIfAbsent(this.className, owner -> HashMultimap.create())
								.put(method, new TypeInstructionIndex(typeInstruction, index));
					}

					index++;
				}
			}
		}

		if (this.classIsLocal) {
			for (final MethodNode method : this.methods) {
				for (final AbstractInsnNode instruction : method.instructions) {
					if (
							instruction instanceof FieldInsnNode fieldInstruction
								&& fieldInstruction.getOpcode() == GETFIELD
					) {
						final FieldIndexOffset fieldOffset = this.localSyntheticFieldOffsetsByDescByNameByOwner
								.getOrDefault(this.className, Map.of())
								.getOrDefault(fieldInstruction.name, Map.of())
								.get(fieldInstruction.desc);

						if (fieldOffset != null) {
							this.localSyntheticFieldsByGetterByOwner
									.computeIfAbsent(this.className, owner -> HashMultimap.create())
									.put(method, fieldOffset.field);
						}
					}
				}
			}
		}
	}

	record TypeInstructionIndex(TypeInsnNode typeInstruction, int index) { }

	record FieldIndexOffset(FieldNode field, int indexOffset) { }
}

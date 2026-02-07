package org.quiltmc.enigma.impl.plugin;

import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParamSyntheticFieldIndexingVisitor extends ClassVisitor {
	private final Map<FieldEntry, LocalVariableEntry> paramsBySyntheticField = new HashMap<>();

	@Nullable
	private ClassEntry clazz;
	private final List<MethodNode> methods = new LinkedList<>();
	private final Map<String, MethodNode> syntheticConstructorsByOwner = new HashMap<>();
	private final Set<String> anonymousClassNames = new HashSet<>();

	ParamSyntheticFieldIndexingVisitor() {
		super(Enigma.ASM_VERSION);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);

		this.clazz = new ClassEntry(name);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		if (innerName == null) {
			// TODO this includes local (named) classes, make sure that's ok
			this.anonymousClassNames.add(name);
		}
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		final MethodNode node = new MethodNode(this.api, access, name, descriptor, signature, exceptions);

		this.methods.add(node);

		return node;
	}

	@Override
	public void visitEnd() {
		super.visitEnd();

		this.collectResults();

		this.clazz = null;
		this.methods.clear();
		this.syntheticConstructorsByOwner.clear();
		this.anonymousClassNames.clear();
	}

	private void collectResults() {
		for (final MethodNode method : this.methods) {
			AbstractInsnNode instruction = method.instructions.getFirst();
			while (instruction != null) {
				if (
						instruction instanceof TypeInsnNode typeInstruction
							&& typeInstruction.getOpcode() == Opcodes.NEW
							&& this.anonymousClassNames.contains(typeInstruction.desc)
				) {
					final List<VarInsnNode> params = new ArrayList<>();
					AbstractInsnNode postTypeInstruction = typeInstruction.getNext();
					while (postTypeInstruction != null) {
						if (
								postTypeInstruction instanceof MethodInsnNode invocation
									&& invocation.name.equals("<init>")
									&& invocation.owner.equals(typeInstruction.desc)
						) {
							// TODO
							break;
						} else if (
								postTypeInstruction instanceof VarInsnNode variable
									// TODO account for non/static and double-size params
									&& variable.var < method.parameters.size()
						) {
							params.add(variable);
						}

						postTypeInstruction = postTypeInstruction.getNext();
					}

					if (postTypeInstruction == null) {
						break;
					} else {
						instruction = postTypeInstruction.getNext();
					}
				} else {
					instruction = instruction.getNext();
				}
			}
		}
	}
}

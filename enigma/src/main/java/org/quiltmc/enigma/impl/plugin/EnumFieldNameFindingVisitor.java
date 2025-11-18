package org.quiltmc.enigma.impl.plugin;

import org.jetbrains.java.decompiler.util.Pair;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EnumFieldNameFindingVisitor extends ClassVisitor {
	private ClassEntry clazz;
	private String className;
	private final Map<FieldEntry, String> enumConstants;
	private final Set<Pair<String, String>> enumFields = new HashSet<>();
	private final List<MethodNode> classInits = new ArrayList<>();

	EnumFieldNameFindingVisitor() {
		super(Enigma.ASM_VERSION);
		this.enumConstants = new HashMap<>();
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.className = name;
		this.clazz = new ClassEntry(name);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if ((access & Opcodes.ACC_ENUM) != 0
				&& !this.enumFields.add(Pair.of(name, descriptor))) {
			throw new IllegalArgumentException("Found two enum fields with the same name \"" + name + "\" and desc \"" + descriptor + "\"!");
		}

		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if ("<clinit>".equals(name)) {
			MethodNode node = new MethodNode(this.api, access, name, descriptor, signature, exceptions);
			this.classInits.add(node);
			return node;
		}

		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}

	@Override
	public void visitEnd() {
		super.visitEnd();
		try {
			this.collectResults();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			this.enumFields.clear();
			this.classInits.clear();
		}
	}

	public boolean isEnumConstant(FieldEntry field) {
		return this.enumConstants.containsKey(field);
	}

	@Nullable
	public String getEnumConstantName(FieldEntry field) {
		return this.enumConstants.get(field);
	}

	private void collectResults() throws Exception {
		String owner = this.className;
		Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());

		for (MethodNode mn : this.classInits) {
			Frame<SourceValue>[] frames = analyzer.analyze(this.className, mn);

			InsnList instrs = mn.instructions;
			for (int i = 1; i < instrs.size(); i++) {
				AbstractInsnNode instr1 = instrs.get(i - 1);
				AbstractInsnNode instr2 = instrs.get(i);
				String s = null;

				if (instr2.getOpcode() == Opcodes.PUTSTATIC
						&& ((FieldInsnNode) instr2).owner.equals(owner)
						&& this.enumFields.contains(Pair.of(((FieldInsnNode) instr2).name, ((FieldInsnNode) instr2).desc))
						&& instr1.getOpcode() == Opcodes.INVOKESPECIAL
						&& "<init>".equals(((MethodInsnNode) instr1).name)) {
					for (int j = 0; j < frames[i - 1].getStackSize(); j++) {
						SourceValue sv = frames[i - 1].getStack(j);
						for (AbstractInsnNode ci : sv.insns) {
							if (ci instanceof LdcInsnNode insnNode && insnNode.cst instanceof String && s == null) {
								s = (String) (insnNode.cst);
							}
						}
					}
				}

				if (s != null) {
					this.enumConstants.put(new FieldEntry(this.clazz, ((FieldInsnNode) instr2).name, new TypeDescriptor(((FieldInsnNode) instr2).desc)), s);
				}

				// report otherwise?
			}
		}
	}
}

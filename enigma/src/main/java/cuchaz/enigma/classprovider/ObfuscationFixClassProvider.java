package cuchaz.enigma.classprovider;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.bytecode.translators.LocalVariableFixVisitor;
import cuchaz.enigma.bytecode.translators.SourceFixVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Wraps a ClassProvider to apply fixes to the following problems introduced by the obfuscator,
 * so that the classes can be decompiled correctly:
 * <ul>
 *	 <li>Bridge methods missing the "bridge" or "synthetic" access modifier
 *	 <li>.getClass() calls added by Proguard (Proguard adds these to preserve semantics when Proguard removes
 *	 a field access which may have caused a {@code NullPointerException}).
 *	 <li>LVT names that don't match parameter table name
 *	 <li>LVT names that are invalid or duplicate
 *	 <li>Enum constructor parameters that are incorrectly named or missing the "synthetic" access modifier
 *	 <li>"this" parameter which is incorrectly named
 *	 <li>Record component fields missing the "private" access modifier
 * </ul>
 * <p>
 * These fixes are only applied to classes that were indexed by the JarIndex provided, and not library classes.
 */
public class ObfuscationFixClassProvider implements ClassProvider {
	private final ClassProvider classProvider;
	private final JarIndex jarIndex;

	public ObfuscationFixClassProvider(ClassProvider classProvider, JarIndex jarIndex) {
		this.classProvider = classProvider;
		this.jarIndex = jarIndex;
	}

	@Override
	@Nullable
	public ClassNode get(String name) {
		ClassNode node = this.classProvider.get(name);

		if (!this.jarIndex.isIndexed(name)) {
			return node;
		}

		ClassNode fixedNode = new ClassNode();
		ClassVisitor visitor = fixedNode;
		visitor = new LocalVariableFixVisitor(Enigma.ASM_VERSION, visitor);
		visitor = new SourceFixVisitor(Enigma.ASM_VERSION, visitor, this.jarIndex);
		node.accept(visitor);
		this.removeRedundantClassCalls(fixedNode);

		return fixedNode;
	}

	@Override
	public Collection<String> getClassNames() {
		return this.classProvider.getClassNames();
	}

	@Override
	public Collection<String> getClasses(String className) {
		return this.classProvider.getClasses(className);
	}

	private void removeRedundantClassCalls(ClassNode node) {
		// Removes .getClass() calls added by Proguard:
		// DUP
		// INVOKEVIRTUAL java/lang/Object.getClass ()Ljava/lang/Class;
		// POP
		for (MethodNode methodNode : node.methods) {
			AbstractInsnNode insnNode = methodNode.instructions.getFirst();
			while (insnNode != null) {
				if (insnNode instanceof MethodInsnNode methodInsnNode && insnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
					if (methodInsnNode.name.equals("getClass") && methodInsnNode.owner.equals("java/lang/Object") && methodInsnNode.desc.equals("()Ljava/lang/Class;")) {
						AbstractInsnNode previous = methodInsnNode.getPrevious();
						AbstractInsnNode next = methodInsnNode.getNext();
						if (previous.getOpcode() == Opcodes.DUP && next.getOpcode() == Opcodes.POP) {
							insnNode = previous.getPrevious();//reset the iterator so it gets the new next instruction
							methodNode.instructions.remove(previous);
							methodNode.instructions.remove(methodInsnNode);
							methodNode.instructions.remove(next);
						}
					}
				}
				insnNode = insnNode.getNext();
			}
		}
	}
}

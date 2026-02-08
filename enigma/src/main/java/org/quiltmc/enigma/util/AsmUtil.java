package org.quiltmc.enigma.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

public class AsmUtil {
	public static byte[] nodeToBytes(ClassNode node) {
		ClassWriter w = new ClassWriter(0);
		node.accept(w);
		return w.toByteArray();
	}

	public static ClassNode bytesToNode(byte[] bytes) {
		ClassReader r = new ClassReader(bytes);
		ClassNode node = new ClassNode();
		r.accept(node, 0);
		return node;
	}

	public static int getLocalIndex(MethodNode node, int local) {
		return getLocalIndex(matchAccess(node, Opcodes.ACC_STATIC), node.desc, local);
	}

	public static int getLocalIndex(boolean isStatic, String desc, int local) {
		var args = Type.getArgumentTypes(desc);
		int size = isStatic ? 0 : 1;

		for (int i = 0; i < args.length; i++) {
			if (local == size) {
				return i;
			} else if (size > local) {
				return -1;
			}

			size += args[i].getSize();
		}

		return -1;
	}

	public static boolean masksMatch(int value, int... masks) {
		for (int mask : masks) {
			if ((value & mask) == 0) {
				return false;
			}
		}

		return true;
	}

	public static boolean matchAccess(MethodNode node, int... masks) {
		return masksMatch(node.access, masks);
	}

	public static boolean matchAccess(ParameterNode node, int... masks) {
		return masksMatch(node.access, masks);
	}
}

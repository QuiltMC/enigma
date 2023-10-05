package org.quiltmc.enigma.impl.analysis.index;

import org.quiltmc.enigma.api.analysis.index.JarIndex;
import org.quiltmc.enigma.impl.analysis.MethodNodeWithAction;
import org.quiltmc.enigma.api.analysis.index.JarIndexer;
import org.quiltmc.enigma.api.translation.representation.ParameterAccessFlags;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class IndexClassVisitor extends ClassVisitor {
	private final JarIndexer indexer;
	private ClassDefEntry classEntry;

	public IndexClassVisitor(JarIndex indexer, int api) {
		super(api);
		this.indexer = indexer;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.classEntry = ClassDefEntry.parse(access, name, signature, superName, interfaces);
		this.indexer.indexClass(this.classEntry);

		super.visit(version, access, name, signature, superName, interfaces);
	}

	// ASM calls the EnclosingMethod attribute "OuterClass"
	@Override
	public void visitOuterClass(String owner, String name, String descriptor) {
		this.indexer.indexEnclosingMethod(this.classEntry, new JarIndexer.EnclosingMethodData(owner, name, descriptor));

		super.visitOuterClass(owner, name, descriptor);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		this.indexer.indexField(FieldDefEntry.parse(this.classEntry, access, name, desc, signature));

		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodDefEntry entry = MethodDefEntry.parse(this.classEntry, access, name, desc, signature);

		return new MethodNodeWithAction(this.api, access, name, desc, signature, exceptions, methodNode -> {
			// add parameter access values to the entry
			if (methodNode.parameters != null) {
				for (int i = 0; i < methodNode.parameters.size(); i++) {
					entry.getDesc().getArgumentDescs().get(i).setAccess(new ParameterAccessFlags(methodNode.parameters.get(i).access));
				}
			}

			this.indexer.indexMethod(entry);
		});
	}
}

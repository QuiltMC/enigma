package cuchaz.enigma.analysis.index;

import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class IndexClassVisitor extends ClassVisitor {
	private final JarIndexer indexer;
	private final EntryIndex entryIndex;
	private ClassDefEntry classEntry;

	public IndexClassVisitor(JarIndexer indexer, EntryIndex entryIndex, int api) {
		super(api);
		this.indexer = indexer;
		this.entryIndex = entryIndex;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.classEntry = this.entryIndex.getDefinition(new ClassEntry(name));

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
		this.indexer.indexMethod(MethodDefEntry.parse(this.classEntry, access, name, desc, signature));

		return super.visitMethod(access, name, desc, signature, exceptions);
	}
}

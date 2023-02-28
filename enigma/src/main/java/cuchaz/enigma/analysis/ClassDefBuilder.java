package cuchaz.enigma.analysis;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;

import cuchaz.enigma.analysis.index.EntryIndex;
import cuchaz.enigma.analysis.index.JarIndexer;
import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.Signature;
import cuchaz.enigma.translation.representation.entry.ClassDefEntry;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public class ClassDefBuilder {

	private final JarIndexer indexer;
	private final EntryIndex entryIndex;
	private final ClassProvider classProvider;

	public ClassDefBuilder(JarIndexer indexer, EntryIndex entryIndex, ClassProvider classProvider) {
		this.indexer = indexer;
		this.entryIndex = entryIndex;
		this.classProvider = classProvider;
	}

	public ClassDefEntry build(String className) {
		return (ClassDefEntry)buildClassEntry(className);
	}

	private ClassEntry buildClassEntry(String className) {
		ClassEntry entry = new ClassEntry(className);
		ClassNode classNode = this.classProvider.get(className);

		if (classNode == null) {
			return entry;
		}

		ClassDefEntry classEntry = this.entryIndex.getDefinition(entry);

		if (classEntry == null) {
			classEntry = buildClassEntry(classNode);
		}

		return classEntry;
	}

	private ClassDefEntry buildClassEntry(ClassNode classNode) {
		ClassEntry outerClass = (classNode.outerClass == null) ? null : buildClassEntry(classNode.outerClass);
		ClassEntry superClass = (classNode.superName == null) ? null : buildClassEntry(classNode.superName);
		ClassEntry[] interfaces = classNode.interfaces.stream().map(this::buildClassEntry).toArray(ClassEntry[]::new);
		Signature signature = Signature.createSignature(classNode.signature);
		AccessFlags access = new AccessFlags(classNode.access);

		ClassDefEntry classEntry = new ClassDefEntry(outerClass, classNode.name, this.findSimpleName(classNode), signature, access, superClass, interfaces);
		this.indexer.indexClass(classEntry);

		return classEntry;
	}

	private String findSimpleName(ClassNode classNode) {
		for (InnerClassNode innerClass : classNode.innerClasses) {
			// classes also hold references to other inner classes
			if (innerClass.name.equals(classNode.name)) {
				return innerClass.innerName;
			}
		}

		return ClassEntry.getSimpleName(classNode.name);
	}
}

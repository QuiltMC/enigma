package cuchaz.enigma.source.bytecode;

import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collection;
import java.util.List;

public class BytecodeDecompiler implements Decompiler {
	private final ClassProvider classProvider;

	public BytecodeDecompiler(ClassProvider classProvider, SourceSettings settings) {
		this.classProvider = classProvider;
	}

	@Override
	public Source getSource(String className, @Nullable EntryRemapper remapper) {
		String rootClassName = className.contains("$") ? className.substring(0, className.indexOf("$")) : className;
		Collection<String> classes = this.classProvider.getClasses(rootClassName);
		List<ClassNode> otherClassNodes = classes.stream().filter(s -> !rootClassName.equals(s)).map(this.classProvider::get).toList();

		return new BytecodeSource(this.classProvider.get(className), otherClassNodes, remapper);
	}
}

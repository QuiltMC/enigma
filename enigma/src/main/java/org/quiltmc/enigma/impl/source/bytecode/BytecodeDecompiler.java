package org.quiltmc.enigma.impl.source.bytecode;

import org.quiltmc.enigma.api.class_provider.ClassProvider;
import org.quiltmc.enigma.api.source.Decompiler;
import org.quiltmc.enigma.api.source.Source;
import org.quiltmc.enigma.api.source.SourceSettings;
import org.quiltmc.enigma.translation.mapping.EntryRemapper;
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

package org.quiltmc.enigma.impl.source.bytecode;

import org.quiltmc.enigma.api.source.Source;
import org.quiltmc.enigma.api.source.SourceIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class BytecodeSource implements Source {
	private final ClassNode classNode;
	private final List<ClassNode> innerClassNodes;
	private final EntryRemapper remapper;

	public BytecodeSource(ClassNode classNode, List<ClassNode> innerClassNodes, EntryRemapper remapper) {
		this.classNode = classNode;
		this.innerClassNodes = innerClassNodes;
		this.remapper = remapper;
	}

	@Override
	public String asString() {
		return this.index().getSource();
	}

	@Override
	public Source withJavadocs(EntryRemapper remapper) {
		return new BytecodeSource(this.classNode, this.innerClassNodes, remapper);
	}

	@Override
	public SourceIndex index() {
		SourceIndex index = new BytecodeSourceIndex();

		EnigmaTextifier textifier = new EnigmaTextifier(index);
		StringWriter out = new StringWriter();
		PrintWriter writer = new PrintWriter(out);

		TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, textifier, writer);

		this.classNode.accept(traceClassVisitor);

		for (ClassNode otherNode : this.innerClassNodes) {
			textifier.clearText();
			writer.println();
			textifier.skipCharacters(1);
			otherNode.accept(traceClassVisitor);
		}

		index.setSource(out.toString());

		return index;
	}
}

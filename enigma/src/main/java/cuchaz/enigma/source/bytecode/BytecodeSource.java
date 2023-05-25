package cuchaz.enigma.source.bytecode;

import cuchaz.enigma.Enigma;
import cuchaz.enigma.bytecode.translators.TranslationClassVisitor;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.translation.mapping.EntryRemapper;
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
		SourceIndex index = new SourceIndex();

		EnigmaTextifier textifier = new EnigmaTextifier(index);
		StringWriter out = new StringWriter();
		PrintWriter writer = new PrintWriter(out);

		TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, textifier, writer);

		ClassNode node = this.classNode;

		if (this.remapper != null) {
			ClassNode translatedNode = new ClassNode();
			node.accept(new TranslationClassVisitor(this.remapper.getDeobfuscator(), Enigma.ASM_VERSION, translatedNode));
			node = translatedNode;
		}

		node.accept(traceClassVisitor);

		for (ClassNode otherNode : this.innerClassNodes) {
			if (this.remapper != null) {
				ClassNode translatedNode = new ClassNode();
				otherNode.accept(new TranslationClassVisitor(this.remapper.getDeobfuscator(), Enigma.ASM_VERSION, translatedNode));
				otherNode = translatedNode;
			}

			textifier.clearText();
			writer.println();
			otherNode.accept(traceClassVisitor);
		}

		index.setSource(out.toString());

		return index;
	}
}

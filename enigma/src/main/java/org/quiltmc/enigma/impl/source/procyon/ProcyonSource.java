package org.quiltmc.enigma.impl.source.procyon;

import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import org.quiltmc.enigma.api.source.Source;
import org.quiltmc.enigma.api.source.SourceIndex;
import org.quiltmc.enigma.impl.source.procyon.index.SourceIndexVisitor;
import org.quiltmc.enigma.impl.source.procyon.transformer.AddJavadocsAstTransform;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;

import java.io.StringWriter;

public class ProcyonSource implements Source {
	private final DecompilerSettings settings;
	private final CompilationUnit tree;
	private String string;

	public ProcyonSource(CompilationUnit tree, DecompilerSettings settings) {
		this.settings = settings;
		this.tree = tree;
	}

	@Override
	public SourceIndex index() {
		SourceIndex index = new SourceIndex(this.asString());
		this.tree.acceptVisitor(new SourceIndexVisitor(), index);
		return index;
	}

	@Override
	public String asString() {
		if (this.string == null) {
			StringWriter writer = new StringWriter();
			this.tree.acceptVisitor(new JavaOutputVisitor(new PlainTextOutput(writer), this.settings), null);
			this.string = writer.toString();
		}

		return this.string;
	}

	@Override
	public Source withJavadocs(EntryRemapper remapper) {
		CompilationUnit remappedTree = (CompilationUnit) this.tree.clone();
		new AddJavadocsAstTransform(remapper).run(remappedTree);
		return new ProcyonSource(remappedTree, this.settings);
	}
}

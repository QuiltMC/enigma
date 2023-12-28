package org.quiltmc.enigma.impl.source.procyon.index;

import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.DepthFirstAstVisitor;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;
import org.quiltmc.enigma.api.source.SourceIndex;
import org.quiltmc.enigma.impl.source.procyon.EntryParser;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;

public class SourceIndexVisitor extends DepthFirstAstVisitor<SourceIndex, Void> {
	@Override
	public Void visitTypeDeclaration(TypeDeclaration node, SourceIndex index) {
		TypeDefinition def = node.getUserData(Keys.TYPE_DEFINITION);
		ClassDefEntry classEntry = EntryParser.parse(def);
		index.addDeclaration(TokenFactory.createToken(index, node.getNameToken()), classEntry);

		return node.acceptVisitor(new SourceIndexClassVisitor(classEntry), index);
	}

	@Override
	protected Void visitChildren(AstNode node, SourceIndex index) {
		for (final AstNode child : node.getChildren()) {
			child.acceptVisitor(this, index);
		}

		return null;
	}
}

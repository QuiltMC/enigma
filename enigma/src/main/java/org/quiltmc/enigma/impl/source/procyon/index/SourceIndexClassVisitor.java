package org.quiltmc.enigma.impl.source.procyon.index;

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.languages.TextLocation;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.ConstructorDeclaration;
import com.strobel.decompiler.languages.java.ast.EnumValueDeclaration;
import com.strobel.decompiler.languages.java.ast.FieldDeclaration;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.MethodDeclaration;
import com.strobel.decompiler.languages.java.ast.SimpleType;
import com.strobel.decompiler.languages.java.ast.TypeDeclaration;
import com.strobel.decompiler.languages.java.ast.VariableInitializer;
import org.quiltmc.enigma.api.source.SourceIndex;
import org.quiltmc.enigma.api.translation.representation.entry.ClassDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldDefEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodDefEntry;
import org.quiltmc.enigma.impl.source.procyon.EntryParser;

public class SourceIndexClassVisitor extends SourceIndexVisitor {
	private final ClassDefEntry classEntry;

	public SourceIndexClassVisitor(ClassDefEntry classEntry) {
		this.classEntry = classEntry;
	}

	@Override
	public Void visitTypeDeclaration(TypeDeclaration node, SourceIndex index) {
		// is this this class, or a subtype?
		TypeDefinition def = node.getUserData(Keys.TYPE_DEFINITION);
		ClassDefEntry classEntry = EntryParser.parse(def);
		if (!classEntry.equals(this.classEntry)) {
			// it's a subtype, recurse
			index.addDeclaration(TokenFactory.createToken(index, node.getNameToken()), classEntry);
			return node.acceptVisitor(new SourceIndexClassVisitor(classEntry), index);
		}

		return this.visitChildren(node, index);
	}

	@Override
	public Void visitSimpleType(SimpleType node, SourceIndex index) {
		TypeReference ref = node.getUserData(Keys.TYPE_REFERENCE);
		if (node.getIdentifierToken().getStartLocation() != TextLocation.EMPTY) {
			ClassEntry entry = new ClassEntry(ref.getInternalName());
			index.addReference(TokenFactory.createToken(index, node.getIdentifierToken()), entry, this.classEntry);
		}

		return this.visitChildren(node, index);
	}

	@Override
	public Void visitMethodDeclaration(MethodDeclaration node, SourceIndex index) {
		MethodDefinition def = node.getUserData(Keys.METHOD_DEFINITION);
		MethodDefEntry methodEntry = EntryParser.parse(def);
		AstNode tokenNode = node.getNameToken();
		if (methodEntry.isConstructor() && methodEntry.getName().equals("<clinit>")) {
			// for static initializers, check elsewhere for the token node
			tokenNode = node.getModifiers().firstOrNullObject();
		}

		index.addDeclaration(TokenFactory.createToken(index, tokenNode), methodEntry);
		return node.acceptVisitor(new SourceIndexMethodVisitor(methodEntry), index);
	}

	@Override
	public Void visitConstructorDeclaration(ConstructorDeclaration node, SourceIndex index) {
		MethodDefinition def = node.getUserData(Keys.METHOD_DEFINITION);
		MethodDefEntry methodEntry = EntryParser.parse(def);
		index.addDeclaration(TokenFactory.createToken(index, node.getNameToken()), methodEntry);
		return node.acceptVisitor(new SourceIndexMethodVisitor(methodEntry), index);
	}

	@Override
	public Void visitFieldDeclaration(FieldDeclaration node, SourceIndex index) {
		FieldDefinition def = node.getUserData(Keys.FIELD_DEFINITION);
		FieldDefEntry fieldEntry = EntryParser.parse(def);
		assert (node.getVariables().size() == 1);
		VariableInitializer variable = node.getVariables().firstOrNullObject();
		index.addDeclaration(TokenFactory.createToken(index, variable.getNameToken()), fieldEntry);
		return this.visitChildren(node, index);
	}

	@Override
	public Void visitEnumValueDeclaration(EnumValueDeclaration node, SourceIndex index) {
		// treat enum declarations as field declarations
		FieldDefinition def = node.getUserData(Keys.FIELD_DEFINITION);
		FieldDefEntry fieldEntry = EntryParser.parse(def);
		index.addDeclaration(TokenFactory.createToken(index, node.getNameToken()), fieldEntry);
		return this.visitChildren(node, index);
	}
}

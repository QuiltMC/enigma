package cuchaz.enigma.source.vineflower;

import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.jetbrains.java.decompiler.main.extern.TextTokenVisitor;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.util.Pair;
import org.jetbrains.java.decompiler.util.token.TextRange;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class EnigmaTextTokenCollector extends TextTokenVisitor {
	private String content;
	private MethodEntry currentMethod;

	private final Map<Token, Entry<?>> declarations = new HashMap<>();
	private final Map<Token, Pair<Entry<?>, Entry<?>>> references = new HashMap<>();
	private final Map<Token, Boolean> tokens = new LinkedHashMap<>();

	public EnigmaTextTokenCollector(TextTokenVisitor next) {
		super(next);
	}

	private static ClassEntry getClassEntry(String name) {
		return new ClassEntry(name);
	}

	private static FieldEntry getFieldEntry(String className, String name, FieldDescriptor descriptor) {
		return FieldEntry.parse(className, name, descriptor.descriptorString);
	}

	private static MethodEntry getMethodEntry(String className, String name, MethodDescriptor descriptor) {
		return MethodEntry.parse(className, name, descriptor.toString());
	}

	private static LocalVariableEntry getParameterEntry(MethodEntry parent, int index, String name) {
		return new LocalVariableEntry(parent, index, name, true, null);
	}

	private static LocalVariableEntry getVariableEntry(MethodEntry parent, int index, String name) {
		return new LocalVariableEntry(parent, index, name, false, null);
	}

	private Token getToken(TextRange range) {
		return new Token(range.start, range.start + range.length, this.content.substring(range.start, range.start + range.length));
	}

	private void addDeclaration(Token token, Entry<?> entry) {
		this.declarations.put(token, entry);
		this.tokens.put(token, true);
	}

	private void addReference(Token token, Entry<?> entry, Entry<?> context) {
		this.references.put(token, Pair.of(entry, context));
		this.tokens.put(token, false);
	}

	public void addTokensToIndex(SourceIndex index, UnaryOperator<Token> tokenProcessor) {
		for (Token token : this.tokens.keySet()) {
			Token newToken = tokenProcessor.apply(token);
			if (newToken == null) {
				continue;
			}

			if (this.tokens.get(token)) {
				index.addDeclaration(newToken, this.declarations.get(token));
			} else {
				Pair<Entry<?>, Entry<?>> ref = this.references.get(token);
				index.addReference(newToken, ref.a, ref.b);
			}
		}
	}

	@Override
	public void start(String content) {
		this.content = content;
		this.currentMethod = null;
	}

	@Override
	public void visitClass(TextRange range, boolean declaration, String name) {
		super.visitClass(range, declaration, name);
		Token token = this.getToken(range);

		if (declaration) {
			this.addDeclaration(token, getClassEntry(name));
		} else {
			this.addReference(token, getClassEntry(name), this.currentMethod);
		}
	}

	@Override
	public void visitField(TextRange range, boolean declaration, String className, String name, FieldDescriptor descriptor) {
		super.visitField(range, declaration, className, name, descriptor);
		Token token = this.getToken(range);

		if (declaration) {
			this.addDeclaration(token, getFieldEntry(className, name, descriptor));
		} else {
			this.addReference(token, getFieldEntry(className, name, descriptor), this.currentMethod);
		}
	}

	@Override
	public void visitMethod(TextRange range, boolean declaration, String className, String name, MethodDescriptor descriptor) {
		super.visitMethod(range, declaration, className, name, descriptor);
		Token token = this.getToken(range);
		MethodEntry entry = getMethodEntry(className, name, descriptor);

		if (token.text.equals("new")) {
			return;
		}

		if (declaration) {
			this.addDeclaration(token, entry);
			this.currentMethod = entry;
		} else {
			this.addReference(token, entry, this.currentMethod);
		}
	}

	@Override
	public void visitParameter(TextRange range, boolean declaration, String className, String methodName, MethodDescriptor methodDescriptor, int idx, String name) {
		super.visitParameter(range, declaration, className, methodName, methodDescriptor, idx, name);
		Token token = this.getToken(range);
		MethodEntry parent = getMethodEntry(className, methodName, methodDescriptor);

		if (declaration) {
			this.addDeclaration(token, getParameterEntry(parent, idx, name));
		} else {
			this.addReference(token, getParameterEntry(parent, idx, name), this.currentMethod);
		}
	}

	@Override
	public void visitLocal(TextRange range, boolean declaration, String className, String methodName, MethodDescriptor methodDescriptor, int idx, String name) {
		super.visitLocal(range, declaration, className, methodName, methodDescriptor, idx, name);
		Token token = this.getToken(range);
		MethodEntry parent = getMethodEntry(className, methodName, methodDescriptor);

		if (declaration) {
			this.addDeclaration(token, getVariableEntry(parent, idx, name));
		} else {
			this.addReference(token, getVariableEntry(parent, idx, name), this.currentMethod);
		}
	}
}

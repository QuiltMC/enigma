package org.quiltmc.enigma.impl.source.vineflower;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.jetbrains.java.decompiler.main.extern.TextTokenVisitor;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.util.Pair;
import org.jetbrains.java.decompiler.util.token.TextRange;
import org.quiltmc.enigma.api.source.SourceIndex;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;

import java.util.*;
import java.util.function.UnaryOperator;

public class EnigmaTextTokenCollector extends TextTokenVisitor {
	private String content;
	private final Deque<ClassEntry> classStack = new ArrayDeque<>();
	private final Deque<MethodEntry> methodStack = new ArrayDeque<>();

	private final Map<Token, Entry<?>> declarations = new HashMap<>();
	private final Map<Token, Pair<Entry<?>, Entry<?>>> references = new HashMap<>();
	private final Map<Token, Boolean> tokens = new LinkedHashMap<>();
	private final Map<ClassEntry, TextRange> classRanges = new HashMap<>();
	private final List<SyntheticMethodSpan> syntheticMethods = new ArrayList<>();
	private final Deque<SyntheticMethodSpan> openSynthetic = new ArrayDeque<>();
	private final Map<SyntheticMethodSpan, MethodEntry> syntheticEntryBySpan = new HashMap<>();

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

	private void parseSource() {
		StaticJavaParser.getParserConfiguration()
			.setStoreTokens(true)
			.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21); //todo see if we can get source information instead of hardcoding this
		CompilationUnit unit = StaticJavaParser.parse(this.content);
		List<InitializerDeclaration> initializers = unit.findAll(InitializerDeclaration.class, InitializerDeclaration::isStatic);
		for (InitializerDeclaration decl : initializers) {
			Range range = decl.getRange().orElseThrow(() -> new IllegalStateException("No range for initializer"));
			int start = positionToIndex(range.begin);
			int end = positionToIndex(range.end);
			syntheticMethods.add(new SyntheticMethodSpan(start, end, false));
		}
		String pkgPrefix = unit.getPackageDeclaration().map(decl -> decl.getNameAsString().replace('.', '/') + "/").orElse("");
		for (TypeDeclaration<?> decl : unit.getTypes()) {
			addClassAndChildren(decl, pkgPrefix + decl.getNameAsString());
		}
	}

	private void addClassAndChildren(TypeDeclaration<?> decl, String name) {
		Range range = decl.getRange().orElseThrow(() -> new IllegalStateException("No range for type declaration"));
		TextRange textRange = new TextRange(positionToIndex(range.begin), positionToIndex(range.end));
		classRanges.put(getClassEntry(name), textRange);
		decl.getMembers().forEach(member -> {
			if (member instanceof TypeDeclaration<?> child) {
				addClassAndChildren(child, name + "$" + child.getNameAsString());
			}
		});
	}

	private int positionToIndex(Position position) {
		int index = 0;
		int linesLeft = position.line;
		while (linesLeft > 0) {
			index = this.content.indexOf('\n', index) + 1;
			linesLeft--;
		}
		return index + position.column;
	}

	private void updateMethodStack(TextRange range) {
		while (!this.openSynthetic.isEmpty() && encloses(this.openSynthetic.peek(), range)) {
			SyntheticMethodSpan span = this.openSynthetic.pop();
			this.syntheticEntryBySpan.remove(span);
			this.methodStack.pop();
		}

		List<SyntheticMethodSpan> enclosing = this.syntheticMethods.stream()
			.filter(span -> encloses(span, range))
			.sorted(Comparator.comparingInt(span -> span.range.length))
			.toList();

		for (SyntheticMethodSpan method : enclosing) {
			if (!this.openSynthetic.contains(method)) {
				MethodEntry entry = this.syntheticEntryBySpan.computeIfAbsent(method, this::getSyntheticMethodEntry);
				if (this.methodStack.isEmpty() || !this.methodStack.peek().equals(entry)) {
					this.methodStack.push(entry);
				}
			}
		}
	}

	private void pruneExitedClass(TextRange range) {
		while (!classStack.isEmpty() && classRanges.get(classStack.peek()).getEnd() < range.start) {
			classStack.pop();
		}
	}

	private static boolean encloses(SyntheticMethodSpan outer, TextRange inner) {
		return outer.range.start < inner.start && outer.range.getEnd() > inner.getEnd();
	}

	private MethodEntry getSyntheticMethodEntry(SyntheticMethodSpan method) {
		if (method.isLambda) {
			//TODO add lambda logic
			throw new UnsupportedOperationException("Lambda handling is not implemented yet");
		} else {
			return getMethodEntry(this.classStack.peek().getFullName(), "<clinit>", MethodDescriptor.parseDescriptor("()V"));
		}
	}

	@Override
	public void start(String content) {
		this.content = content;
		this.classRanges.clear();
		this.methodStack.clear();
		this.openSynthetic.clear();
		this.syntheticMethods.clear();
		this.syntheticEntryBySpan.clear();
		parseSource();
	}

	@Override
	public void visitClass(TextRange range, boolean declaration, String name) {
		super.visitClass(range, declaration, name);
		Token token = this.getToken(range);
		pruneExitedClass(range);
		updateMethodStack(range);

		if (declaration) {
			this.classStack.push(getClassEntry(name));
			this.addDeclaration(token, getClassEntry(name));
		} else {
			this.addReference(token, getClassEntry(name), this.methodStack.peek());
		}
	}

	@Override
	public void visitField(TextRange range, boolean declaration, String className, String name, FieldDescriptor descriptor) {
		super.visitField(range, declaration, className, name, descriptor);
		Token token = this.getToken(range);
		pruneExitedClass(range);
		updateMethodStack(range);

		if (declaration) {
			this.addDeclaration(token, getFieldEntry(className, name, descriptor));
		} else {
			this.addReference(token, getFieldEntry(className, name, descriptor), this.methodStack.peek());
		}
	}

	@Override
	public void visitMethod(TextRange range, boolean declaration, String className, String name, MethodDescriptor descriptor) {
		super.visitMethod(range, declaration, className, name, descriptor);
		Token token = this.getToken(range);
		pruneExitedClass(range);
		updateMethodStack(range);
		MethodEntry entry = getMethodEntry(className, name, descriptor);

		if (declaration) {
			this.addDeclaration(token, entry);
			if (!this.methodStack.isEmpty()) {
				this.methodStack.pop();
			}

			this.methodStack.push(entry);
		} else {
			this.addReference(token, entry, this.methodStack.peek());
		}
	}

	@Override
	public void visitParameter(TextRange range, boolean declaration, String className, String methodName, MethodDescriptor methodDescriptor, int idx, String name) {
		super.visitParameter(range, declaration, className, methodName, methodDescriptor, idx, name);
		Token token = this.getToken(range);
		pruneExitedClass(range);
		updateMethodStack(range);
		MethodEntry parent = getMethodEntry(className, methodName, methodDescriptor);

		if (declaration) {
			this.addDeclaration(token, getParameterEntry(parent, idx, name));
		} else {
			this.addReference(token, getParameterEntry(parent, idx, name), this.methodStack.peek());
		}
	}

	@Override
	public void visitLocal(TextRange range, boolean declaration, String className, String methodName, MethodDescriptor methodDescriptor, int idx, String name) {
		super.visitLocal(range, declaration, className, methodName, methodDescriptor, idx, name);
		Token token = this.getToken(range);
		pruneExitedClass(range);
		updateMethodStack(range);
		MethodEntry parent = getMethodEntry(className, methodName, methodDescriptor);

		if (declaration) {
			this.addDeclaration(token, getVariableEntry(parent, idx, name));
		} else {
			this.addReference(token, getVariableEntry(parent, idx, name), this.methodStack.peek());
		}
	}

	private record SyntheticMethodSpan(TextRange range, boolean isLambda) {
		public SyntheticMethodSpan(int start, int end, boolean isLambda) {
			this(new TextRange(start, end - start), isLambda);
		}
	}
}

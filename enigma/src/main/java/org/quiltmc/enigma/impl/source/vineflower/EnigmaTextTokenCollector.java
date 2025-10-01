package org.quiltmc.enigma.impl.source.vineflower;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.code.Instruction;
import org.jetbrains.java.decompiler.code.InstructionSequence;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.TextTokenVisitor;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.attr.StructBootstrapMethodsAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.struct.consts.LinkConstant;
import org.jetbrains.java.decompiler.struct.consts.PooledConstant;
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
import org.quiltmc.enigma.util.LineIndexer;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class EnigmaTextTokenCollector extends TextTokenVisitor {
	private String content;
	private LineIndexer lineIndexer;
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
		ParserConfiguration config = new ParserConfiguration()
				.setStoreTokens(true)
				.setLanguageLevel(ParserConfiguration.LanguageLevel.RAW);

		ParseResult<CompilationUnit> parseResult = new JavaParser(config).parse(this.content);
		if (!parseResult.isSuccessful()) {
			Logger.warn("Failed to parse source: {}", parseResult.getProblems());
			return;
		}

		CompilationUnit unit = parseResult.getResult().get();
		List<InitializerDeclaration> initializers = unit.findAll(InitializerDeclaration.class, InitializerDeclaration::isStatic);
		for (InitializerDeclaration decl : initializers) {
			TextRange range = this.getTextRangeForNode(decl);
			if (range == null) {
				continue;
			}

			this.syntheticMethods.add(new SyntheticMethodSpan(range, false));
		}

		for (FieldDeclaration decl : unit.findAll(FieldDeclaration.class, FieldDeclaration::isStatic)) {
			TextRange range = this.getTextRangeForNode(decl);
			if (range == null) {
				continue;
			}

			this.syntheticMethods.add(new SyntheticMethodSpan(range, false));
		}

		String pkgPrefix = unit.getPackageDeclaration().map(decl -> decl.getNameAsString().replace('.', '/') + "/").orElse("");
		for (TypeDeclaration<?> decl : unit.getTypes()) {
			this.addClassAndChildren(decl, pkgPrefix + decl.getNameAsString());
		}

		for (ClassEntry classEntry : this.classRanges.keySet()) {
			String[] parts = classEntry.getContextualName().split("\\$");
			TypeDeclaration<?> type = null;
			for (TypeDeclaration<?> decl : unit.getTypes()) {
				if (decl.getNameAsString().equals(parts[0])) {
					type = decl;
					break;
				}
			}

			for (int i = 1; i < parts.length; i++) {
				if (type != null) {
					TypeDeclaration<?> finalType = type;
					String name = parts[i];
					type = type.findFirst(TypeDeclaration.class, t -> t != finalType && t.getNameAsString().equals(name)).orElse(null);
				}
			}

			if (type == null) {
				throw new IllegalStateException("Could not find type " + classEntry.getContextualName() + " in parsed source");
			}

			Map<String, LambdaNode> rootNodes = new HashMap<>();
			for (var member : type.getMembers()) {
				if (member instanceof TypeDeclaration<?>) {
					continue;
				}

				if (member instanceof ConstructorDeclaration constructor) {
					LambdaNode rootNode = rootNodes.computeIfAbsent("<init>", c -> new LambdaNode());
					this.findLambdasInSource(constructor.getBody(), rootNode);
				} else if (member instanceof MethodDeclaration method) {
					if (method.getBody().isPresent()) {
						LambdaNode rootNode = rootNodes.computeIfAbsent(method.getNameAsString(), c -> new LambdaNode());
						this.findLambdasInSource(method.getBody().get(), rootNode);
					}
				} else {
					LambdaNode rootNode = rootNodes.computeIfAbsent("<clinit>", c -> new LambdaNode());
					for (LambdaExpr lambda : member.findAll(LambdaExpr.class)) {
						if (lambda.findAncestor(LambdaExpr.class).isEmpty()) {
							LambdaNode lambdaNode = new LambdaNode(lambda, false);
							rootNode.children.add(lambdaNode);
							this.findLambdasInSource(lambda, rootNode);
						}
					}
				}
			}

			StructClass clazz = DecompilerContext.getStructContext().getClass(classEntry.getFullName());
			if (clazz == null) {
				throw new IllegalStateException("Class bytecode not found");
			}

			for (StructMethod method : clazz.getMethods()) {
				LambdaNode rootNode = rootNodes.get(method.getName());
				if (rootNode == null) {
					continue;
				}

				this.pairContext(clazz, method, rootNode);
			}
		}
	}

	private void pairContext(StructClass owner, StructMethod method, LambdaNode rootNode) {
		List<MethodEntry> bytecodeLambdas = extractLambdasFromBytecode(owner, method);
		int count = Math.min(bytecodeLambdas.size(), rootNode.children.size());
		for (int i = 0; i < count; i++) {
			LambdaNode childNode = rootNode.children.get(i);
			if (childNode.isMethodReference) {
				continue;
			}

			MethodEntry entry = bytecodeLambdas.get(i);
			if (childNode.range == null) {
				continue;
			}

			SyntheticMethodSpan span = new SyntheticMethodSpan(childNode.range, true);
			this.syntheticMethods.add(span);
			this.syntheticEntryBySpan.put(span, entry);
			StructClass entryClass = DecompilerContext.getStructContext().getClass(entry.getParent().getFullName());
			StructMethod entryMethod = entryClass.getMethod(entry.getName(), entry.getDesc().toString());
			this.pairContext(entryClass, entryMethod, childNode);
		}
	}

	private void findLambdasInSource(Node method, LambdaNode parentNode) {
		for (var member : method.getChildNodes()) {
			if (member instanceof LambdaExpr lambda) {
				LambdaNode lambdaNode = new LambdaNode(lambda, false);
				parentNode.children.add(lambdaNode);
				this.findLambdasInSource(lambda, lambdaNode);
			} else if (member instanceof MethodReferenceExpr methodRef) {
				LambdaNode lambdaNode = new LambdaNode(methodRef, true);
				parentNode.children.add(lambdaNode);
			} else {
				this.findLambdasInSource(member, parentNode);
			}
		}
	}

	private static List<MethodEntry> extractLambdasFromBytecode(StructClass clazz, StructMethod method) {
		List<MethodEntry> lambdas = new ArrayList<>();
		ConstantPool pool = clazz.getPool();

		StructBootstrapMethodsAttribute bootstrapAttr = clazz.getAttribute(StructGeneralAttribute.ATTRIBUTE_BOOTSTRAP_METHODS);

		if (bootstrapAttr == null) {
			return lambdas;
		}

		if (!method.containsCode()) {
			return lambdas;
		}

		try {
			method.expandData(clazz);
		} catch (IOException e) {
			return lambdas;
		}

		InstructionSequence seq = method.getInstructionSequence();
		if (seq == null) {
			return lambdas;
		}

		for (int i = 0; i < seq.length(); i++) {
			Instruction instr = seq.getInstr(i);
			if (instr.opcode != CodeConstants.opc_invokedynamic) {
				continue;
			}

			int indyIndex = instr.operand(0);
			PooledConstant constant = pool.getConstant(indyIndex);
			if (!(constant instanceof LinkConstant link) || link.type != LinkConstant.CONSTANT_InvokeDynamic) {
				continue;
			}

			int bsmIndex = link.index1;
			LinkConstant bootstrapMethod = bootstrapAttr.getMethodReference(bsmIndex);
			String methodOwner = bootstrapMethod.classname;
			String methodName = bootstrapMethod.elementname;
			boolean isLambda = "java/lang/invoke/LambdaMetafactory".equals(methodOwner)
								&& ("metafactory".equals(methodName) || "altMetafactory".equals(methodName));
			if (!isLambda) {
				continue;
			}

			List<PooledConstant> args = bootstrapAttr.getMethodArguments(bsmIndex);

			if (args.size() < 3) {
				continue;
			}

			PooledConstant implConstant = args.get(1);
			if (!(implConstant instanceof LinkConstant implMethod)) {
				continue;
			}

			String owner = implMethod.classname;
			String name = implMethod.elementname;
			String descriptor = implMethod.descriptor;

			lambdas.add(getMethodEntry(owner, name, MethodDescriptor.parseDescriptor(descriptor)));
		}

		method.releaseResources();

		return lambdas;
	}

	private void addClassAndChildren(TypeDeclaration<?> decl, String name) {
		TextRange textRange = this.getTextRangeForNode(decl);
		if (textRange == null) {
			return;
		}

		this.classRanges.put(getClassEntry(name), textRange);
		decl.getMembers().forEach(member -> {
			if (member instanceof TypeDeclaration<?> child) {
				this.addClassAndChildren(child, name + "$" + child.getNameAsString());
			}
		});
	}

	private TextRange getTextRangeForNode(Node node) {
		Optional<Range> rangeOpt = node.getRange();
		if (rangeOpt.isEmpty()) {
			Logger.error("No range for node of type {}", node.getClass().getSimpleName());
			return null;
		}

		Range range = rangeOpt.get();
		int start = this.lineIndexer.getIndex(range.begin);
		int end = this.lineIndexer.getIndex(range.end);
		return new TextRange(start, end - start);
	}

	private void updateMethodStack(TextRange range) {
		while (!this.openSynthetic.isEmpty() && !encloses(this.openSynthetic.peek(), range)) {
			SyntheticMethodSpan span = this.openSynthetic.pop();
			this.syntheticEntryBySpan.remove(span);
			this.methodStack.pop();
		}

		List<SyntheticMethodSpan> enclosing = this.syntheticMethods.stream()
				.filter(span -> encloses(span, range))
				.sorted(Comparator.<SyntheticMethodSpan>comparingInt(span -> span.range.length).reversed())
				.toList();

		for (SyntheticMethodSpan method : enclosing) {
			if (!this.openSynthetic.contains(method)) {
				MethodEntry entry = this.syntheticEntryBySpan.computeIfAbsent(method, this::getSyntheticMethodEntry);
				if (this.methodStack.isEmpty() || !this.methodStack.peek().equals(entry)) {
					this.methodStack.push(entry);
					this.openSynthetic.push(method);
				}
			}
		}
	}

	private void pruneExitedClasses(TextRange range) {
		if (this.classRanges.isEmpty()) {
			return; // Parsing failed
		}

		while (!this.classStack.isEmpty() && this.classRanges.get(this.classStack.peek()).getEnd() < range.start) {
			this.classStack.pop();
		}
	}

	private static boolean encloses(SyntheticMethodSpan outer, TextRange inner) {
		return outer.range.start <= inner.start && outer.range.getEnd() >= inner.getEnd();
	}

	private MethodEntry getSyntheticMethodEntry(SyntheticMethodSpan method) {
		if (method.isLambda) {
			throw new IllegalStateException("Method entries for lambdas should have already been fetched");
		} else {
			if (this.classStack.isEmpty()) {
				throw new IllegalStateException("No class on the stack for synthetic method at " + method.range);
			}

			return getMethodEntry(this.classStack.peek().getFullName(), "<clinit>", MethodDescriptor.parseDescriptor("()V"));
		}
	}

	@Override
	public void start(String content) {
		this.content = content;
		this.lineIndexer = new LineIndexer(content);
		this.classRanges.clear();
		this.methodStack.clear();
		this.openSynthetic.clear();
		this.syntheticMethods.clear();
		this.syntheticEntryBySpan.clear();
		this.parseSource();
	}

	@Override
	public void visitClass(TextRange range, boolean declaration, String name) {
		super.visitClass(range, declaration, name);
		Token token = this.getToken(range);
		this.pruneExitedClasses(range);
		this.updateMethodStack(range);

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
		this.pruneExitedClasses(range);
		this.updateMethodStack(range);

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
		this.pruneExitedClasses(range);
		this.updateMethodStack(range);
		MethodEntry entry = getMethodEntry(className, name, descriptor);

		if (declaration) {
			this.addDeclaration(token, entry);
			if (!this.methodStack.isEmpty()) {
				this.methodStack.pop();
			}

			this.methodStack.push(entry);
		} else {
			MethodEntry context = !this.methodStack.isEmpty() ? this.methodStack.peek() : getMethodEntry(className, "<clinit>", MethodDescriptor.parseDescriptor("()V"));
			this.addReference(token, entry, context);
		}
	}

	@Override
	public void visitParameter(TextRange range, boolean declaration, String className, String methodName, MethodDescriptor methodDescriptor, int idx, String name) {
		super.visitParameter(range, declaration, className, methodName, methodDescriptor, idx, name);
		Token token = this.getToken(range);
		this.pruneExitedClasses(range);
		this.updateMethodStack(range);
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
		this.pruneExitedClasses(range);
		this.updateMethodStack(range);
		MethodEntry parent = getMethodEntry(className, methodName, methodDescriptor);

		if (declaration) {
			this.addDeclaration(token, getVariableEntry(parent, idx, name));
		} else {
			this.addReference(token, getVariableEntry(parent, idx, name), this.methodStack.peek());
		}
	}

	private record SyntheticMethodSpan(TextRange range, boolean isLambda) {}

	class LambdaNode {
		final TextRange range;
		final boolean isMethodReference;
		final List<LambdaNode> children = new ArrayList<>();
		LambdaNode(Expression lambda, boolean isMethodReference) {
			this.range = EnigmaTextTokenCollector.this.getTextRangeForNode(lambda);
			this.isMethodReference = isMethodReference;
		}

		LambdaNode() {
			this.range = null;
			this.isMethodReference = false;
		}
	}
}

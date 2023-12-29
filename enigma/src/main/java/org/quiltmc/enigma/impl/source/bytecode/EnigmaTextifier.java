package org.quiltmc.enigma.impl.source.bytecode;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.util.Printer;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.source.SourceIndex;
import org.objectweb.asm.util.Textifier;
import org.quiltmc.enigma.api.source.Token;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.Either;
import org.quiltmc.enigma.util.Pair;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnigmaTextifier extends Textifier {
	sealed interface QueuedToken {
		record Declaration(Entry<?> entry) implements QueuedToken {
		}

		record Reference(Entry<?> entry, Entry<?> context) implements QueuedToken {
		}

		record Descriptor(String descriptor) implements QueuedToken {
			@Override
			public boolean shouldSkip() {
				return !this.descriptor.contains("L");
			}
		}

		record MethodDescriptor(String descriptor) implements QueuedToken {
		}

		record Signature() implements QueuedToken {
		}

		record OffsetToken(int offset, String text, QueuedToken token) implements QueuedToken {
		}

		record Array(QueuedToken... tokens) implements QueuedToken {
		}

		record Skip() implements QueuedToken {
		}

		default boolean shouldSkip() {
			return this instanceof Skip || this instanceof Signature; // Skip signatures for now
		}
	}

	record PartialToken(int start, String text, Either<Entry<?>, Pair<Entry<?>, Entry<?>>> declarationOrReference) {
		PartialToken(int start, String text, Entry<?> entry) {
			this(start, text, Either.left(entry));
		}

		PartialToken(int start, String text, Entry<?> entry, Entry<?> context) {
			this(start, text, Either.right(new Pair<>(entry, context)));
		}
	}

	private static final String MODULE_WARNING = "  // Warning: Modules are not supported\n";

	private final SourceIndex sourceIndex;

	private final Deque<QueuedToken> tokenQueue = new ArrayDeque<>();
	private final List<List<PartialToken>> tokensPerText = new ArrayList<>();
	private final Map<Integer, EnigmaTextifier> childTextifiers = new HashMap<>();

	private ClassEntry currentClass;
	private MethodEntry currentMethod;
	private int totalOffset = 0;

	public EnigmaTextifier(SourceIndex sourceIndex) {
		super(Enigma.ASM_VERSION);
		this.sourceIndex = sourceIndex;
	}

	public void clearText() {
		this.text.clear();
		this.tokensPerText.clear();
		this.childTextifiers.clear();
	}

	// region class printer

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.currentClass = new BytecodeClassEntry(name);

		this.queueToken(new QueuedToken.Signature()); // class signature
		this.queueToken(new QueuedToken.Declaration(this.currentClass)); // class name
		if (superName != null && !"java/lang/Object".equals(superName)) {
			this.queueToken(new QueuedToken.Reference(new BytecodeClassEntry(superName), this.currentMethod)); // extended class name
		}

		if (interfaces != null) {
			for (String interfaceName : interfaces) {
				this.queueToken(new QueuedToken.Reference(new BytecodeClassEntry(interfaceName), this.currentMethod)); // implemented interface name
			}
		}

		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public void visitNestHost(String nestHost) {
		this.queueToken(new QueuedToken.Reference(new BytecodeClassEntry(nestHost), this.currentMethod)); // nest host

		super.visitNestHost(nestHost);
	}

	@Override
	public void visitOuterClass(String owner, String name, String descriptor) {
		// JVMS§4.7.7
		var ownerEntry = new BytecodeClassEntry(owner);
		this.queueToken(new QueuedToken.Reference(ownerEntry, this.currentMethod)); // outer class

		if (name != null) {
			this.queueToken(new QueuedToken.Array(
				new QueuedToken.OffsetToken(-name.length() - 1, name,
					new QueuedToken.Reference(new MethodEntry(ownerEntry, name, new MethodDescriptor(descriptor)), this.currentMethod)), // enclosing method (by its name)
				new QueuedToken.MethodDescriptor(descriptor) // enclosing method descriptor
			));
		}

		super.visitOuterClass(owner, name, descriptor);
	}

	@Override
	public void visitNestMember(String nestMember) {
		this.queueToken(new QueuedToken.Reference(new BytecodeClassEntry(nestMember), this.currentMethod));

		super.visitNestMember(nestMember);
	}

	@Override
	public void visitPermittedSubclass(String permittedSubclass) {
		this.queueToken(new QueuedToken.Reference(new BytecodeClassEntry(permittedSubclass), this.currentMethod));

		super.visitPermittedSubclass(permittedSubclass);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		// JVMS§4.7.6
		this.queueToken(new QueuedToken.Reference(new BytecodeClassEntry(name), this.currentMethod));
		this.queueToken(outerName != null ? new QueuedToken.Reference(new BytecodeClassEntry(outerName), this.currentMethod) : new QueuedToken.Skip());

		super.visitInnerClass(name, outerName, innerName, access);
	}

	@Override
	public Printer visitRecordComponent(String name, String descriptor, String signature) {
		if (signature != null) {
			this.queueToken(new QueuedToken.Signature()); // component signature
		}

		this.queueToken(new QueuedToken.Array(
			new QueuedToken.MethodDescriptor(descriptor), // component descriptor
			new QueuedToken.OffsetToken(descriptor.length() + 1, name,
				new QueuedToken.Reference(new FieldEntry(this.currentClass, name, new TypeDescriptor(descriptor)), this.currentMethod)) // component field
		));

		return super.visitRecordComponent(name, descriptor, signature);
	}

	@Override
	public Textifier visitField(int access, String name, String descriptor, String signature, Object value) {
		if (signature != null) {
			this.queueToken(new QueuedToken.Signature());
		}

		this.queueToken(new QueuedToken.Array(
			new QueuedToken.Descriptor(descriptor), // field descriptor
			new QueuedToken.OffsetToken(descriptor.length() + 1, name,
				new QueuedToken.Declaration(new FieldEntry(this.currentClass, name, new TypeDescriptor(descriptor)))) // field (on its name)
		));

		return super.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public Textifier visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (signature != null) {
			this.queueToken(new QueuedToken.Signature());
		}

		var entry = new MethodEntry(this.currentClass, name, new MethodDescriptor(descriptor));
		this.queueToken(new QueuedToken.Array(
			new QueuedToken.OffsetToken(-name.length(), name,
				new QueuedToken.Declaration(entry)), // method (on its name)
			new QueuedToken.MethodDescriptor(descriptor) // method descriptor
		));

		if (exceptions != null) {
			for (var exception : exceptions) {
				this.queueToken(new QueuedToken.Reference(new BytecodeClassEntry(exception), this.currentMethod)); // thrown exception
			}
		}

		var child = super.visitMethod(access, name, descriptor, signature, exceptions);
		((EnigmaTextifier) child).currentMethod = entry;
		return child;
	}

	@Override
	public void visitClassEnd() {
		super.visitClassEnd();
		this.fillTokensPerText();
	}

	// endregion

	// region module printer

	@Override
	public void visitMainClass(String mainClass) {
		this.text.add(MODULE_WARNING);
		super.visitMainClass(mainClass);
	}

	@Override
	public void visitModuleEnd() {
		super.visitModuleEnd();
		this.fillTokensPerText();
	}

	// endregion

	// region annotation printer

	@Override
	public Textifier visitAnnotation(String name, String descriptor) {
		this.queueToken(new QueuedToken.Descriptor(descriptor));

		return super.visitAnnotation(name, descriptor);
	}

	@Override
	public void visitAnnotationEnd() {
		super.visitAnnotationEnd();
		this.fillTokensPerText();
	}

	// endregion

	// region record component printer

	@Override
	public void visitRecordComponentEnd() {
		super.visitRecordComponentEnd();
		this.fillTokensPerText();
	}

	// endregion

	// region field printer

	@Override
	public void visitFieldEnd() {
		super.visitFieldEnd();
		this.fillTokensPerText();
	}

	// endregion

	// region method printer

	@Override
	public void visitTypeInsn(int opcode, String type) {
		this.queueToken(new QueuedToken.Reference(new BytecodeClassEntry(type), this.currentMethod));

		super.visitTypeInsn(opcode, type);
	}

	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		var clazz = new BytecodeClassEntry(owner);
		this.queueToken(new QueuedToken.Array(
			new QueuedToken.Reference(clazz, this.currentMethod), // field owner
			new QueuedToken.OffsetToken(owner.length() + 1, name,
				new QueuedToken.Reference(new FieldEntry(clazz, name, new TypeDescriptor(descriptor)), this.currentMethod)) // field (by its name)
		));
		this.queueToken(new QueuedToken.Descriptor(descriptor)); // field descriptor

		super.visitFieldInsn(opcode, owner, name, descriptor);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		var clazz = new BytecodeClassEntry(owner);
		this.queueToken(new QueuedToken.Array(
			new QueuedToken.Reference(clazz, this.currentMethod), // method owner
			new QueuedToken.OffsetToken(owner.length() + 1, name,
				new QueuedToken.Reference(new MethodEntry(clazz, name, new MethodDescriptor(descriptor)), this.currentMethod)) // method (by its name)
		));
		this.queueToken(new QueuedToken.MethodDescriptor(descriptor)); // method descriptor

		super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
	}

	@Override
	public void visitInvokeDynamicInsn(String name, String descriptor, Handle bsmHandle, Object... bootstrapMethodArguments) {
		this.queueToken(new QueuedToken.Descriptor(descriptor)); // indy descriptor

		// bsm
		boolean isMethodHandle = bsmHandle.getTag() >= Opcodes.H_INVOKEVIRTUAL && bsmHandle.getTag() <= Opcodes.H_INVOKEINTERFACE;
		var bsmOwner = new ClassEntry(bsmHandle.getOwner());
		var desc = bsmHandle.getDesc();
		this.queueToken(new QueuedToken.Array(
			new QueuedToken.Reference(bsmOwner, null), // bsm owner
			new QueuedToken.OffsetToken(bsmOwner.getFullName().length() + 1, bsmHandle.getName(),
				new QueuedToken.Reference(isMethodHandle
					? new MethodEntry(bsmOwner, bsmHandle.getName(), new MethodDescriptor(desc)) // bsm handle method
					: new FieldEntry(bsmOwner, bsmHandle.getName(), new TypeDescriptor(desc)), this.currentMethod)) // bsm handle field
		));

		this.queueToken(isMethodHandle ? new QueuedToken.MethodDescriptor(desc) : new QueuedToken.Descriptor(desc)); // bsm handle descriptor

		super.visitInvokeDynamicInsn(name, descriptor, bsmHandle, bootstrapMethodArguments);
	}

	@Override
	public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
		this.queueToken(new QueuedToken.Descriptor(descriptor));

		super.visitMultiANewArrayInsn(descriptor, numDimensions);
	}

	@Override
	public Printer visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		this.queueToken(new QueuedToken.Descriptor(descriptor));

		return super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
	}

	@Override
	public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
		this.queueToken(new QueuedToken.Descriptor(descriptor));

		if (signature != null) {
			this.queueToken(new QueuedToken.Signature());
		}

		super.visitLocalVariable(name, descriptor, signature, start, end, index);
	}

	@Override
	public Printer visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
		this.queueToken(new QueuedToken.Descriptor(descriptor));

		return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
	}

	@Override
	public void visitMethodEnd() {
		super.visitMethodEnd();
		this.fillTokensPerText();
	}

	// endregion

	// region printer extra

	@Override
	public Textifier visitAnnotation(String descriptor, boolean visible) {
		this.queueToken(new QueuedToken.Descriptor(descriptor));

		return super.visitAnnotation(descriptor, visible);
	}

	@Override
	public Textifier visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		this.queueToken(new QueuedToken.Descriptor(descriptor));

		return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
	}

	// endregion

	private void queueToken(QueuedToken token) {
		this.tokenQueue.add(token);
	}

	@Override
	protected void appendDescriptor(int type, String value) {
		int tokenStart = this.stringBuilder.length();

		super.appendDescriptor(type, value);

		var queuedToken = this.tokenQueue.poll();
		this.addToken(this.getCurrentTokens(), value, tokenStart, queuedToken);
	}

	private void addToken(List<PartialToken> tokens, String text, int tokenStart, QueuedToken queuedToken) {
		if (queuedToken == null || queuedToken.shouldSkip()) {
			return;
		} else if (queuedToken instanceof QueuedToken.Array array) {
			for (var token : array.tokens) {
				this.addToken(tokens, text, tokenStart, token);
			}

			return;
		} else if (queuedToken instanceof QueuedToken.OffsetToken t) {
			this.addToken(tokens, t.text, tokenStart + t.offset, t.token);
			return;
		}

		// This would be far simpler with pattern matching
		if (queuedToken instanceof QueuedToken.Declaration d) {
			tokens.add(new PartialToken(tokenStart, text, d.entry));
		} else if (queuedToken instanceof QueuedToken.Reference r) {
			tokens.add(new PartialToken(tokenStart, text, r.entry, r.context));
		} else if (queuedToken instanceof QueuedToken.Descriptor d) {
			var clazz = d.descriptor.substring(d.descriptor.indexOf('L') + 1, d.descriptor.length() - 1);
			tokens.add(new PartialToken(tokenStart + 1, clazz, new BytecodeClassEntry(clazz), null));
		} else if (queuedToken instanceof QueuedToken.MethodDescriptor d) {
			for (int i = 1; i < d.descriptor.length(); i++) {
				char c = d.descriptor.charAt(i);
				if (c == 'L') {
					int start = i + 1;
					int end = d.descriptor.indexOf(';', start);
					var clazz = d.descriptor.substring(start, end);

					tokens.add(new PartialToken(tokenStart + start, clazz, new BytecodeClassEntry(clazz), null));
					i = end;
				}
			}
		}
	}

	private List<PartialToken> getCurrentTokens() {
		int texts = this.text.size();
		if (this.tokensPerText.size() < texts) {
			this.fillTokensPerText();
		} else if (this.tokensPerText.size() > texts) {
			return this.tokensPerText.get(texts);
		}

		List<PartialToken> tokens = new ArrayList<>();
		this.tokensPerText.add(tokens);
		return tokens;
	}

	private void fillTokensPerText() {
		for (int i = this.tokensPerText.size(); i < this.text.size(); i++) {
			this.tokensPerText.add(List.of());
		}
	}

	@Override
	public void print(PrintWriter printWriter) {
		this.printText(printWriter);
	}

	private void printText(PrintWriter printWriter) {
		int offset = this.totalOffset;
		for (int i = 0; i < this.text.size(); i++) {
			var o = this.text.get(i);

			if (o instanceof List<?> l) {
				offset = this.printChildTextifier(printWriter, l, offset, this.childTextifiers.get(i));
			} else {
				var s = o.toString();
				printWriter.print(s);

				if (i > this.tokensPerText.size() - 1) {
					throw new IllegalStateException("Too few token lists");
				}

				var tokens = this.tokensPerText.get(i);
				for (var partialToken : tokens) {
					int start = partialToken.start + offset;
					var token = new Token(start, start + partialToken.text.length(), partialToken.text);
					partialToken.declarationOrReference.ifLeft(e -> this.sourceIndex.addDeclaration(token, e));
					partialToken.declarationOrReference.ifRight(p -> this.sourceIndex.addReference(token, p.a(), p.b()));
				}

				offset += s.length();
			}
		}

		this.totalOffset = offset;
	}

	private int printChildTextifier(PrintWriter printWriter, List<?> text, int offset, EnigmaTextifier textifier) {
		if (textifier == null) {
			throw new IllegalStateException("null child textifier");
		} else if (textifier.getText() != text) {
			throw new IllegalStateException("The provided text doesn't correspond to the provided textifier");
		}

		textifier.totalOffset = offset;
		textifier.printText(printWriter);
		return textifier.totalOffset;
	}

	@Override
	protected Textifier createTextifier() {
		var textifier = new EnigmaTextifier(this.sourceIndex);
		textifier.currentClass = this.currentClass;
		this.childTextifiers.put(this.text.size(), textifier);
		return textifier;
	}

	public void skipCharacters(int n) {
		this.totalOffset += n;
	}
}

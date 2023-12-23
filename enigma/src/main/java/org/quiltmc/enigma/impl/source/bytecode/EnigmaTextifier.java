package org.quiltmc.enigma.impl.source.bytecode;

import org.objectweb.asm.Attribute;
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

import java.util.ArrayDeque;
import java.util.Deque;

public class EnigmaTextifier extends Textifier {
	private final SourceIndex sourceIndex;

	sealed interface QueuedToken {
		record Declaration(Entry<?> entry) implements QueuedToken {
		}

		record Reference(Entry<?> entry, Entry<?> context) implements QueuedToken {
		}

		record Descriptor(String descriptor, Entry<?> context) implements QueuedToken {
			@Override
			public boolean shouldSkip() {
				return !this.descriptor.startsWith("L");
			}
		}

		record MethodDescriptor(String descriptor, Entry<?> context) implements QueuedToken {
		}

		record Signature() implements QueuedToken {
		} // TODO

		record OffsetToken(int offset, String text, QueuedToken token) implements QueuedToken {
		}

		record Multiple(QueuedToken... tokens) implements QueuedToken {
		}

		record Skip() implements QueuedToken {
		}

		default boolean shouldSkip() {
			return this instanceof Skip || this instanceof Signature; // Skip signatures for now
		}
	}

	/**
	 * A queue to collect tokens on {@link #appendDescriptor(int, String)}.
	 */
	private final Deque<QueuedToken> tokenQueue = new ArrayDeque<>();

	private ClassEntry currentClass;
	private MethodEntry currentMethod;
	private int currentOffset = 0;

	public EnigmaTextifier(SourceIndex sourceIndex) {
		super(Enigma.ASM_VERSION);
		this.sourceIndex = sourceIndex;
	}

	public void clearText() {
		this.text.clear();
	}

	private void updateOffset() {
		if (this.text.isEmpty()) {
			return;
		}

		var lastText = this.text.get(this.text.size() - 1);
		if (lastText instanceof String s) {
			this.currentOffset += s.length();
		}
	}

	// region class printer
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.currentClass = new ClassEntry(name);

		this.queueToken(new QueuedToken.Signature()); // class signature
		this.queueToken(new QueuedToken.Declaration(this.currentClass)); // class name
		if (superName != null && !"java/lang/Object".equals(superName)) {
			this.queueToken(new QueuedToken.Reference(new ClassEntry(superName), this.currentMethod)); // extended class name
		}

		if (interfaces != null) {
			for (String interfaceName : interfaces) {
				this.queueToken(new QueuedToken.Reference(new ClassEntry(interfaceName), this.currentMethod)); // implemented interface name
			}
		}

		super.visit(version, access, name, signature, superName, interfaces);
		this.updateOffset();
	}

	@Override
	public void visitSource(String file, String debug) {
		super.visitSource(file, debug);
		this.updateOffset();
	}

	@Override
	public Printer visitModule(String name, int access, String version) {
		var r = super.visitModule(name, access, version);
		this.updateOffset();
		return r; // TODO
	}

	@Override
	public void visitNestHost(String nestHost) {
		this.queueToken(new QueuedToken.Reference(new ClassEntry(nestHost), this.currentMethod)); // nest host

		super.visitNestHost(nestHost);
		this.updateOffset();
	}

	@Override
	public void visitOuterClass(String owner, String name, String descriptor) {
		// JVMS§4.7.7
		var ownerEntry = new ClassEntry(owner);
		this.queueToken(new QueuedToken.Reference(ownerEntry, this.currentMethod)); // outer class

		if (name != null) {
			this.queueToken(new QueuedToken.Multiple(
				new QueuedToken.OffsetToken(-name.length() - 1, name,
					new QueuedToken.Reference(new MethodEntry(ownerEntry, name, new MethodDescriptor(descriptor)), this.currentMethod)), // enclosing method (by its name)
				new QueuedToken.MethodDescriptor(descriptor, this.currentMethod) // enclosing method descriptor
			));
		}

		super.visitOuterClass(owner, name, descriptor);
		this.updateOffset();
	}

	@Override
	public Textifier visitClassAnnotation(String descriptor, boolean visible) {
		var r = super.visitClassAnnotation(descriptor, visible);
		this.updateOffset();
		return r;
	}

	@Override
	public Printer visitClassTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		var r = super.visitClassTypeAnnotation(typeRef, typePath, descriptor, visible);
		this.updateOffset();
		return r;
	}

	@Override
	public void visitClassAttribute(Attribute attribute) {
		this.currentOffset++; // can't do updateOffset since the last text after visiting is the attribute info, not the newline
		super.visitClassAttribute(attribute);
	}

	@Override
	public void visitNestMember(String nestMember) {
		this.queueToken(new QueuedToken.Reference(new ClassEntry(nestMember), this.currentMethod));
		super.visitNestMember(nestMember);
		this.currentOffset += this.stringBuilder.length();
	}

	@Override
	public void visitPermittedSubclass(String permittedSubclass) {
		this.queueToken(new QueuedToken.Reference(new ClassEntry(permittedSubclass), this.currentMethod));
		super.visitPermittedSubclass(permittedSubclass);
		this.currentOffset += this.stringBuilder.length();
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		// JVMS§4.7.6
		this.queueToken(new QueuedToken.Reference(new ClassEntry(name), this.currentMethod));
		this.queueToken(outerName != null ? new QueuedToken.Reference(new ClassEntry(outerName), this.currentMethod) : new QueuedToken.Skip());
		super.visitInnerClass(name, outerName, innerName, access);
		this.currentOffset += this.stringBuilder.length();
	}

	@Override
	public Printer visitRecordComponent(String name, String descriptor, String signature) {
		if (signature != null) {
			this.queueToken(new QueuedToken.Signature()); // component signature
		}

		this.queueToken(new QueuedToken.Multiple(
			new QueuedToken.MethodDescriptor(descriptor, this.currentMethod), // component descriptor
			new QueuedToken.OffsetToken(descriptor.length() + 1, name,
				new QueuedToken.Reference(new FieldEntry(this.currentClass, name, new TypeDescriptor(descriptor)), this.currentMethod)) // component field
		));

		var r = super.visitRecordComponent(name, descriptor, signature);
		this.currentOffset += this.stringBuilder.length();
		return r;
	}

	@Override
	public Textifier visitField(int access, String name, String descriptor, String signature, Object value) {
		if (signature != null) {
			this.queueToken(new QueuedToken.Signature());
		}

		this.queueToken(new QueuedToken.Multiple(
			new QueuedToken.Descriptor(descriptor, this.currentMethod), // field descriptor
			new QueuedToken.OffsetToken(descriptor.length() + 1, name,
				new QueuedToken.Reference(new FieldEntry(this.currentClass, name, new TypeDescriptor(descriptor)), this.currentMethod)) // field (on its name)
		));

		var r = super.visitField(access, name, descriptor, signature, value);
		this.currentOffset += this.stringBuilder.length();
		return r; // TODO
	}

	@Override
	public Textifier visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (signature != null) {
			this.queueToken(new QueuedToken.Signature());
		}

		this.queueToken(new QueuedToken.Multiple(
			new QueuedToken.OffsetToken(-name.length(), name,
				new QueuedToken.Reference(new MethodEntry(this.currentClass, name, new MethodDescriptor(descriptor)), this.currentMethod)), // method (on its name)
			new QueuedToken.Descriptor(descriptor, this.currentMethod) // method descriptor
		));

		if (exceptions != null) {
			for (var exception : exceptions) {
				this.queueToken(new QueuedToken.Reference(new ClassEntry(exception), this.currentMethod)); // thrown exception
			}
		}

		var r = super.visitMethod(access, name, descriptor, signature, exceptions);
		this.currentOffset += this.stringBuilder.length();
		return r; // TODO
	}

	@Override
	public void visitClassEnd() {
		super.visitClassEnd();
	}

	// endregion

	@Override
	public void visitAttribute(Attribute attribute) {
		super.visitAttribute(attribute);
		this.updateOffset();
	}

	private void queueToken(QueuedToken token) {
		this.tokenQueue.add(token);
	}

	@Override
	protected void appendDescriptor(int type, String value) {
		int tokenOffset = this.stringBuilder.length();

		super.appendDescriptor(type, value);

		var queuedToken = this.tokenQueue.poll();
		this.addToken(value, tokenOffset, queuedToken);
	}

	private void addToken(String value, int tokenOffset, QueuedToken queuedToken) {
		if (queuedToken == null || queuedToken.shouldSkip()) {
			return;
		} else if (queuedToken instanceof QueuedToken.Multiple multiple) {
			for (var token : multiple.tokens) {
				this.addToken(value, tokenOffset, token);
			}

			return;
		} else if (queuedToken instanceof QueuedToken.OffsetToken t) {
			this.addToken(t.text, tokenOffset + t.offset, t.token);
			return;
		}

		int start = this.currentOffset + tokenOffset;
		var token = new Token(start, start + value.length(), value);

		// This would be far simpler with pattern matching
		if (queuedToken instanceof QueuedToken.Declaration d) {
			this.sourceIndex.addDeclaration(token, d.entry);
		} else if (queuedToken instanceof QueuedToken.Reference r) {
			this.sourceIndex.addReference(token, r.entry, r.context);
		} else if (queuedToken instanceof QueuedToken.Descriptor d) {
			token.start += 1;
			token.end -= 1;
			token.text = d.descriptor.substring(1, d.descriptor.length() - 1);
			this.sourceIndex.addReference(token, new ClassEntry(token.text), d.context);
		} else if (queuedToken instanceof QueuedToken.Signature s) {
			// TODO
		}
	}

	@Override
	protected Textifier createTextifier() {
		return super.createTextifier();
	}
}

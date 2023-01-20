package cuchaz.enigma.translation.representation;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

import cuchaz.enigma.bytecode.translators.TranslationSignatureVisitor;
import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public class Signature implements Translatable {
	private static final Pattern OBJECT_PATTERN = Pattern.compile(".*:Ljava/lang/Object;:.*");

	private final String signature;
	private final boolean isType;

	private Signature(String signature, boolean isType) {
		if (signature != null && OBJECT_PATTERN.matcher(signature).matches()) {
			signature = signature.replaceAll(":Ljava/lang/Object;:", "::");
		}

		this.signature = signature;
		this.isType = isType;
	}

	public static Signature createTypedSignature(String signature) {
		if (signature != null && !signature.isEmpty()) {
			return new Signature(signature, true);
		}
		return new Signature(null, true);
	}

	public static Signature createSignature(String signature) {
		if (signature != null && !signature.isEmpty()) {
			return new Signature(signature, false);
		}
		return new Signature(null, false);
	}

	public String getSignature() {
		return this.signature;
	}

	public boolean isType() {
		return this.isType;
	}

	public Signature remap(UnaryOperator<String> remapper) {
		if (this.signature == null) {
			return this;
		}
		SignatureWriter writer = new SignatureWriter();
		SignatureVisitor visitor = new TranslationSignatureVisitor(remapper, writer);
		if (this.isType) {
			new SignatureReader(this.signature).acceptType(visitor);
		} else {
			new SignatureReader(this.signature).accept(visitor);
		}
		return new Signature(writer.toString(), this.isType);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Signature other) {
			return (other.signature == null && this.signature == null || other.signature != null
					&& this.signature != null && other.signature.equals(this.signature))
					&& other.isType == this.isType;
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hash = (this.isType ? 1 : 0) << 16;
		if (this.signature != null) {
			hash |= this.signature.hashCode();
		}

		return hash;
	}

	@Override
	public String toString() {
		return this.signature;
	}

	@Override
	public TranslateResult<Signature> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		return TranslateResult.ungrouped(this.remap(name -> translator.translate(new ClassEntry(name)).getFullName()));
	}
}

package cuchaz.enigma.translation.representation.entry;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

import cuchaz.enigma.source.RenamableTokenType;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.TypeDescriptor;

public class LocalVariableDefEntry extends LocalVariableEntry {
	protected final TypeDescriptor desc;

	public LocalVariableDefEntry(MethodEntry ownerEntry, int index, String name, boolean parameter, TypeDescriptor desc, String javadoc) {
		super(ownerEntry, index, name, parameter, javadoc);
		Preconditions.checkNotNull(desc, "Variable desc cannot be null");

		this.desc = desc;
	}

	public TypeDescriptor getDesc() {
		return this.desc;
	}

	@Override
	protected TranslateResult<LocalVariableEntry> extendedTranslate(Translator translator, @Nonnull EntryMapping mapping) {
		TypeDescriptor translatedDesc = translator.translate(this.desc);
		String translatedName = mapping.targetName() != null ? mapping.targetName() : this.name;
		String javadoc = mapping.javadoc();
		return TranslateResult.of(
				mapping.targetName() == null ? RenamableTokenType.OBFUSCATED : RenamableTokenType.DEOBFUSCATED,
				new LocalVariableDefEntry(this.parent, this.index, translatedName, this.parameter, translatedDesc, javadoc)
		);
	}

	@Override
	public LocalVariableDefEntry withName(String name) {
		return new LocalVariableDefEntry(this.parent, this.index, name, this.parameter, this.desc, this.javadocs);
	}

	@Override
	public LocalVariableDefEntry withParent(MethodEntry entry) {
		return new LocalVariableDefEntry(entry, this.index, this.name, this.parameter, this.desc, this.javadocs);
	}

	@Override
	public String toString() {
		return this.parent + "(" + this.index + ":" + this.name + ":" + this.desc + ")";
	}
}

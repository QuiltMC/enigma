package cuchaz.enigma.translation.representation;

import cuchaz.enigma.source.RenamableTokenType;
import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.translation.representation.entry.ParentedEntry;

import java.util.Objects;

public record Lambda(String invokedName, MethodDescriptor invokedType, MethodDescriptor samMethodType, ParentedEntry<?> implMethod, MethodDescriptor instantiatedMethodType) implements Translatable {
	@Override
	public TranslateResult<Lambda> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		MethodEntry samMethod = new MethodEntry(this.getInterface(), this.invokedName, this.samMethodType);
		EntryMapping samMethodMapping = this.resolveMapping(resolver, mappings, samMethod);

		return TranslateResult.of(
				samMethodMapping.targetName() == null ? RenamableTokenType.OBFUSCATED : RenamableTokenType.DEOBFUSCATED,
				new Lambda(
						samMethodMapping.targetName() != null ? samMethodMapping.targetName() : this.invokedName,
						this.invokedType.extendedTranslate(translator, resolver, mappings).getValue(),
						this.samMethodType.extendedTranslate(translator, resolver, mappings).getValue(),
						this.implMethod.extendedTranslate(translator, resolver, mappings).getValue(),
						this.instantiatedMethodType.extendedTranslate(translator, resolver, mappings).getValue()
				)
		);
	}

	private EntryMapping resolveMapping(EntryResolver resolver, EntryMap<EntryMapping> mappings, MethodEntry methodEntry) {
		for (MethodEntry entry : resolver.resolveEntry(methodEntry, ResolutionStrategy.RESOLVE_ROOT)) {
			EntryMapping mapping = mappings.get(entry);
			if (mapping != null) {
				return mapping;
			}
		}

		return EntryMapping.DEFAULT;
	}

	public ClassEntry getInterface() {
		return this.invokedType.getReturnDesc().getTypeEntry();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || this.getClass() != o.getClass()) return false;
		Lambda lambda = (Lambda) o;
		return Objects.equals(this.invokedName, lambda.invokedName)
				&& Objects.equals(this.invokedType, lambda.invokedType)
				&& Objects.equals(this.samMethodType, lambda.samMethodType)
				&& Objects.equals(this.implMethod, lambda.implMethod)
				&& Objects.equals(this.instantiatedMethodType, lambda.instantiatedMethodType);
	}

	@Override
	public String toString() {
		return "Lambda{"
				+ "invokedName='" + this.invokedName + '\''
				+ ", invokedType=" + this.invokedType
				+ ", samMethodType=" + this.samMethodType
				+ ", implMethod=" + this.implMethod
				+ ", instantiatedMethodType="
				+ this.instantiatedMethodType
				+ '}';
	}
}

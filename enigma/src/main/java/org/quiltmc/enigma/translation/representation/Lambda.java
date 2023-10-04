package org.quiltmc.enigma.translation.representation;

import org.quiltmc.enigma.source.RenamableTokenType;
import org.quiltmc.enigma.translation.Translatable;
import org.quiltmc.enigma.translation.TranslateResult;
import org.quiltmc.enigma.translation.Translator;
import org.quiltmc.enigma.translation.mapping.EntryMap;
import org.quiltmc.enigma.translation.mapping.EntryMapping;
import org.quiltmc.enigma.translation.mapping.EntryResolver;
import org.quiltmc.enigma.translation.mapping.ResolutionStrategy;
import org.quiltmc.enigma.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.translation.representation.entry.ParentedEntry;

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

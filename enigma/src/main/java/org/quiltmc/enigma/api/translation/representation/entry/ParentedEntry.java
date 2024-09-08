package org.quiltmc.enigma.api.translation.representation.entry;

import com.google.common.base.Preconditions;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMap;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryResolver;
import org.quiltmc.enigma.api.translation.mapping.ResolutionStrategy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ParentedEntry<P extends Entry<?>> implements Entry<P> {
	protected final P parent;
	protected final String name;
	protected final @Nullable String javadocs;

	protected ParentedEntry(P parent, String name, String javadocs) {
		this.parent = parent;
		this.name = name;
		this.javadocs = javadocs;

		Preconditions.checkNotNull(name, "Name cannot be null");
	}

	@Override
	public abstract ParentedEntry<P> withParent(P parent);

	@Override
	public abstract ParentedEntry<P> withName(String name);

	protected abstract TranslateResult<? extends ParentedEntry<P>> extendedTranslate(Translator translator, @Nonnull EntryMapping mapping);

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getSimpleName() {
		return this.name;
	}

	@Override
	public String getFullName() {
		return this.parent.getFullName() + "." + this.name;
	}

	@Override
	public String getContextualName() {
		return this.parent.getContextualName() + "." + this.name;
	}

	@Override
	@Nullable
	public P getParent() {
		return this.parent;
	}

	@Nullable
	@Override
	public String getJavadocs() {
		return this.javadocs;
	}

	@Override
	public TranslateResult<? extends ParentedEntry<P>> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		EntryMapping mapping = this.resolveMapping(resolver, mappings);
		if (this.getParent() == null) {
			return this.extendedTranslate(translator, mapping);
		}

		P translatedParent = translator.translate(this.getParent());
		return this.withParent(translatedParent).extendedTranslate(translator, mapping);
	}

	private EntryMapping resolveMapping(EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		for (ParentedEntry<P> entry : resolver.resolveEntry(this, ResolutionStrategy.RESOLVE_ROOT)) {
			EntryMapping mapping = mappings.get(entry);
			if (mapping != null) {
				return mapping;
			}
		}

		return EntryMapping.OBFUSCATED;
	}
}

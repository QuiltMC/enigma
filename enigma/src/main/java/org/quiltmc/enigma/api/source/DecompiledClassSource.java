package org.quiltmc.enigma.api.source;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.analysis.EntryReference;
import org.quiltmc.enigma.api.translation.TranslateResult;
import org.quiltmc.enigma.api.translation.Translator;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.LocalVariableDefEntry;
import org.quiltmc.enigma.impl.translation.LocalNameGenerator;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DecompiledClassSource {
	public static boolean DEBUG_TOKEN_HIGHLIGHTS = false;

	private final ClassEntry classEntry;

	private final SourceIndex obfuscatedIndex;
	private final SourceIndex remappedIndex;

	private final TokenStore highlightedTokens;

	private DecompiledClassSource(ClassEntry classEntry, SourceIndex obfuscatedIndex, SourceIndex remappedIndex, TokenStore highlightedTokens) {
		this.classEntry = classEntry;
		this.obfuscatedIndex = obfuscatedIndex;
		this.remappedIndex = remappedIndex;
		this.highlightedTokens = highlightedTokens;
	}

	public DecompiledClassSource(ClassEntry classEntry, SourceIndex index) {
		this(classEntry, index, index, TokenStore.empty());
	}

	public static DecompiledClassSource text(ClassEntry classEntry, String text) {
		return new DecompiledClassSource(classEntry, new SourceIndex(text));
	}

	public DecompiledClassSource remapSource(EnigmaProject project, Translator translator) {
		SourceRemapper remapper = new SourceRemapper(this.obfuscatedIndex.getSource(), this.obfuscatedIndex.referenceTokens());

		TokenStore tokenStore = TokenStore.create(this.obfuscatedIndex);
		SourceRemapper.Result remapResult = remapper.remap((token, movedToken) -> this.remapToken(tokenStore, project, token, movedToken, translator));
		SourceIndex remappedIndex = this.obfuscatedIndex.remapTo(remapResult);
		return new DecompiledClassSource(this.classEntry, this.obfuscatedIndex, remappedIndex, tokenStore);
	}

	private String remapToken(TokenStore target, EnigmaProject project, Token token, Token movedToken, Translator translator) {
		EntryReference<Entry<?>, Entry<?>> reference = this.obfuscatedIndex.getReference(token);

		Entry<?> entry = this.obfuscatedIndex.remapToNameable ? reference.getNameableEntry() : reference.entry;
		TranslateResult<Entry<?>> translatedEntry = translator.extendedTranslate(entry);

		if (project.isRenamable(reference)) {
			if (translatedEntry != null && !translatedEntry.isObfuscated()) {
				target.add(project, translatedEntry.getMapping(), movedToken);
				return translatedEntry.getValue().getSourceRemapName();
			} else {
				target.add(project, EntryMapping.OBFUSCATED, movedToken);
			}
		} else if (DEBUG_TOKEN_HIGHLIGHTS) {
			target.add(project, new EntryMapping(null, null, TokenType.DEBUG, null), movedToken);
		}

		return this.generateDefaultName(translatedEntry.getValue());
	}

	@Nullable
	private String generateDefaultName(Entry<?> entry) {
		if (entry instanceof LocalVariableDefEntry localVariable) {
			int index = localVariable.getIndex();
			if (localVariable.isArgument()) {
				List<TypeDescriptor> arguments = localVariable.getParent().getDesc().getTypeDescs();
				return LocalNameGenerator.generateArgumentName(index, localVariable.getDesc(), arguments);
			} else {
				return LocalNameGenerator.generateLocalVariableName(index, localVariable.getDesc());
			}
		}

		return null;
	}

	public ClassEntry getEntry() {
		return this.classEntry;
	}

	public SourceIndex getIndex() {
		return this.remappedIndex;
	}

	public TokenStore getTokenStore() {
		return this.highlightedTokens;
	}

	public Map<TokenType, ? extends Collection<Token>> getHighlightedTokens() {
		return this.highlightedTokens.getByType();
	}

	public int getObfuscatedOffset(int deobfOffset) {
		return getOffset(this.remappedIndex, this.obfuscatedIndex, deobfOffset);
	}

	public int getDeobfuscatedOffset(int obfOffset) {
		return getOffset(this.obfuscatedIndex, this.remappedIndex, obfOffset);
	}

	private static int getOffset(SourceIndex fromIndex, SourceIndex toIndex, int fromOffset) {
		int relativeOffset = 0;

		Iterator<Token> fromTokenItr = fromIndex.referenceTokens().iterator();
		Iterator<Token> toTokenItr = toIndex.referenceTokens().iterator();
		while (fromTokenItr.hasNext() && toTokenItr.hasNext()) {
			Token fromToken = fromTokenItr.next();
			Token toToken = toTokenItr.next();
			if (fromToken.end > fromOffset) {
				break;
			}

			relativeOffset = toToken.end - fromToken.end;
		}

		return fromOffset + relativeOffset;
	}

	@Override
	public String toString() {
		return this.remappedIndex.getSource();
	}
}

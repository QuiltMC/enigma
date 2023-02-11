package cuchaz.enigma.source;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SourceIndex {
	private String source;
	private List<Integer> lineOffsets;
	private final TreeMap<Token, EntryReference<Entry<?>, Entry<?>>> tokenToReference;
	private final Multimap<EntryReference<Entry<?>, Entry<?>>, Token> referenceToTokens;
	private final Map<Entry<?>, Token> declarationToToken;

	public SourceIndex() {
		this.tokenToReference = new TreeMap<>();
		this.referenceToTokens = HashMultimap.create();
		this.declarationToToken = Maps.newHashMap();
	}

	public SourceIndex(String source) {
		this();
		this.setSource(source);
	}

	public void setSource(String source) {
		this.source = source;
		this.lineOffsets = new ArrayList<>();
		this.lineOffsets.add(0);

		for (int i = 0; i < this.source.length(); i++) {
			if (this.source.charAt(i) == '\n') {
				this.lineOffsets.add(i + 1);
			}
		}
	}

	public String getSource() {
		return this.source;
	}

	public int getLineNumber(int position) {
		int line = 0;

		for (int offset : this.lineOffsets) {
			if (offset > position) {
				break;
			}

			line++;
		}

		return line;
	}

	public int getColumnNumber(int position) {
		return position - this.lineOffsets.get(this.getLineNumber(position) - 1) + 1;
	}

	public int getPosition(int line, int column) {
		return this.lineOffsets.get(line - 1) + column - 1;
	}

	public Iterable<Entry<?>> declarations() {
		return this.declarationToToken.keySet();
	}

	public Iterable<Token> declarationTokens() {
		return this.declarationToToken.values();
	}

	public Token getDeclarationToken(Entry<?> entry) {
		return this.declarationToToken.get(entry);
	}

	public void addDeclaration(Token token, Entry<?> deobfEntry) {
		if (token != null) {
			EntryReference<Entry<?>, Entry<?>> reference = new EntryReference<>(deobfEntry, token.text);
			this.tokenToReference.put(token, reference);
			this.referenceToTokens.put(reference, token);
			this.referenceToTokens.put(EntryReference.declaration(deobfEntry, token.text), token);
			this.declarationToToken.put(deobfEntry, token);
		}
	}

	public Iterable<EntryReference<Entry<?>, Entry<?>>> references() {
		return this.referenceToTokens.keySet();
	}

	public EntryReference<Entry<?>, Entry<?>> getReference(Token token) {
		if (token == null) {
			return null;
		}

		return this.tokenToReference.get(token);
	}

	public Iterable<Token> referenceTokens() {
		return this.tokenToReference.keySet();
	}

	public Token getReferenceToken(int pos) {
		Token token = this.tokenToReference.floorKey(new Token(pos, pos, null));

		if (token != null && token.contains(pos)) {
			return token;
		}

		return null;
	}

	public Collection<Token> getReferenceTokens(EntryReference<Entry<?>, Entry<?>> deobfReference) {
		return this.referenceToTokens.get(deobfReference);
	}

	public void addReference(Token token, Entry<?> deobfEntry, Entry<?> deobfContext) {
		if (token != null) {
			EntryReference<Entry<?>, Entry<?>> deobfReference = new EntryReference<>(deobfEntry, token.text, deobfContext);
			this.tokenToReference.put(token, deobfReference);
			this.referenceToTokens.put(deobfReference, token);
		}
	}

	public void resolveReferences(EntryResolver resolver) {
		// resolve all the classes in the source references
		for (Token token : new ArrayList<>(this.referenceToTokens.values())) {
			EntryReference<Entry<?>, Entry<?>> reference = this.tokenToReference.get(token);
			EntryReference<Entry<?>, Entry<?>> resolvedReference = resolver.resolveFirstReference(reference, ResolutionStrategy.RESOLVE_CLOSEST);

			// replace the reference
			this.tokenToReference.replace(token, resolvedReference);

			Collection<Token> tokens = this.referenceToTokens.removeAll(reference);
			this.referenceToTokens.putAll(resolvedReference, tokens);
		}
	}

	public SourceIndex remapTo(SourceRemapper.Result result) {
		SourceIndex remapped = new SourceIndex(result.getSource());

		for (Map.Entry<Entry<?>, Token> entry : this.declarationToToken.entrySet()) {
			remapped.declarationToToken.put(entry.getKey(), result.getRemappedToken(entry.getValue()));
		}

		for (Map.Entry<EntryReference<Entry<?>, Entry<?>>, Collection<Token>> entry : this.referenceToTokens.asMap().entrySet()) {
			EntryReference<Entry<?>, Entry<?>> reference = entry.getKey();
			Collection<Token> oldTokens = entry.getValue();

			Collection<Token> newTokens = oldTokens
					.stream()
					.map(result::getRemappedToken)
					.toList();

			remapped.referenceToTokens.putAll(reference, newTokens);
		}

		for (Map.Entry<Token, EntryReference<Entry<?>, Entry<?>>> entry : this.tokenToReference.entrySet()) {
			remapped.tokenToReference.put(result.getRemappedToken(entry.getKey()), entry.getValue());
		}

		return remapped;
	}
}

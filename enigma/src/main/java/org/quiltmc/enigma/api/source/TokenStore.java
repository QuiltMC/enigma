package org.quiltmc.enigma.api.source;

import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

public final class TokenStore {
	private static final TokenStore EMPTY = new TokenStore(Collections.emptyNavigableSet(), Collections.emptyMap(), Collections.emptyNavigableSet(), null);

	private final NavigableSet<Token> tokens;
	private final Map<TokenType, NavigableSet<Token>> byType;
	private final NavigableSet<Token> fallbackTokens;
	private final String obfSource;

	private TokenStore(NavigableSet<Token> tokens, Map<TokenType, NavigableSet<Token>> byType, NavigableSet<Token> fallbackTokens, String obfSource) {
		this.tokens = tokens;
		this.byType = byType;
		this.fallbackTokens = fallbackTokens;
		this.obfSource = obfSource;
	}

	public static TokenStore create(SourceIndex obfuscatedIndex) {
		EnumMap<TokenType, NavigableSet<Token>> map = new EnumMap<>(TokenType.class);
		for (TokenType value : TokenType.values()) {
			map.put(value, new TreeSet<>(Comparator.comparing(t -> t.start)));
		}

		return new TokenStore(new TreeSet<>(Comparator.comparing(t -> t.start)), Collections.unmodifiableMap(map), new TreeSet<>(Comparator.comparing(t -> t.start)), obfuscatedIndex.getSource());
	}

	public static TokenStore empty() {
		return TokenStore.EMPTY;
	}

	public void add(EnigmaProject project, EntryMapping mapping, Token token) {
		this.tokens.add(token);
		this.byType.get(mapping.tokenType()).add(token);

		if (mapping.sourcePluginId() != null) {
			var sourceServiceOptional = project.getEnigma().getService(NameProposalService.TYPE, mapping.sourcePluginId());
			sourceServiceOptional.ifPresent(service -> {
				if (service.isFallback()) {
					this.fallbackTokens.add(token);
				}
			});
		}
	}

	public boolean isFallback(Token token) {
		return this.fallbackTokens.contains(token);
	}

	public boolean isCompatible(TokenStore other) {
		return this.obfSource != null && other.obfSource != null
				&& this.obfSource.equals(other.obfSource)
				&& this.tokens.size() == other.tokens.size()
				&& this.fallbackTokens.size() == other.fallbackTokens.size();
	}

	public int mapPosition(TokenStore to, int position) {
		if (!this.isCompatible(to)) return 0;

		int newPos = position;
		Iterator<Token> thisIter = this.tokens.iterator();
		Iterator<Token> toIter = to.tokens.iterator();
		while (thisIter.hasNext()) {
			Token token = thisIter.next();
			Token newToken = toIter.next();

			if (position < token.start) break;

			// if we're inside the token and the text changed,
			// snap the cursor to the beginning
			if (!token.text.equals(newToken.text) && position < token.end) {
				newPos = newToken.start;
				break;
			}

			newPos += newToken.length() - token.length();
		}

		return newPos;
	}

	public Map<TokenType, NavigableSet<Token>> getByType() {
		return this.byType;
	}
}

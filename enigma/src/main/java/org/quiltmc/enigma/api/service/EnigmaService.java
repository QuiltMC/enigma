package org.quiltmc.enigma.api.service;

public interface EnigmaService {
	/**
	 * The ID of this service. This should satisfy a few criteria:
	 * <ul>
	 *     <li>Be namespaced, with the plugin name and service name separated by a colon. The {@code enigma} namespace is reserved for builtin plugins.</li>
	 *     <li>Be all lowercase, with words separated by underscores. Slashes are allowed only after the namespace.</li>
	 *     <li>Be constant and unique: it should never change.</li>
	 * </ul>
	 * <p>Examples: {@code enigma:cfr}, {@code enigma:enum_proposers/name}, {@code your_plugin:custom_indexer}</p>
	 *
	 * @return the constant ID
	 */
	String getId();
}

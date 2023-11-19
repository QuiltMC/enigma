package org.quiltmc.enigma.api.service;

public interface EnigmaService {
	/**
	 * The ID of this service. This should satisfy a few criteria:
	 * <ul>
	 *     <li>Be namespaced, with the plugin name and service name separated by a colon.</li>
	 *     <li>Be all lowercase, with words separated by underscores.</li>
	 *     <li>Be constant and unique: it should never change.</li>
	 * </ul>
	 * @return the constant ID
	 */
	String getId();
}
